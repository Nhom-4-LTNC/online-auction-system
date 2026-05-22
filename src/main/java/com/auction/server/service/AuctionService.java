package com.auction.server.service;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.exception.AuthorizationException;
import com.auction.shared.exception.ResourceNotFoundException;
import com.auction.shared.util.ItemFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionService {

    private static volatile AuctionService instance;

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private AuctionService() {
        try {
            for (Auction auction : auctionRepository.getAllAuctions()) {
                auctions.put(auction.getId(), auction);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService] Không thể tải dữ liệu phiên đấu giá từ DB: " + e.getMessage());
        }
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) instance = new AuctionService();
            }
        }
        return instance;
    }

    public synchronized Auction createAuction(int sellerId, ItemDTO itemDto, double bidStep, long endTimeMillis) throws Exception {
        long startTimeMillis = System.currentTimeMillis();
        AuctionValidator.validateAuctionParams(startTimeMillis, endTimeMillis, bidStep);

        String imageUrl = ItemService.getInstance().saveImage(
                itemDto != null ? itemDto.getImageData() : null,
                itemDto != null ? itemDto.getImageFileName() : null
        );

        Auction newAuction;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                User seller = userRepository.getUserById(conn, sellerId);
                if (seller == null) {
                    throw new ResourceNotFoundException("Người dùng (Người bán)", sellerId);
                }

                Item newItem = ItemFactory.createItem(itemDto, seller, 0, imageUrl);
                AuctionValidator.validateItem(newItem);
                itemRepository.addItem(conn, newItem);

                newAuction = new Auction(newItem, bidStep, startTimeMillis, endTimeMillis);
                auctionRepository.addAuction(conn, newAuction);

                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        updateCachedAuction(newAuction);
        return newAuction;
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[AuctionService] Cannot rollback transaction: "
                    + rollbackError.getMessage());
        }
    }

    private void restoreAutoCommitQuietly(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreError) {
            System.err.println("[AuctionService] Cannot restore autoCommit: "
                    + restoreError.getMessage());
        }
    }

    public Auction getAuctionModelById(int auctionId) throws Exception {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
        }

        return auction;
    }

    public void updateCachedAuction(Auction auction) {
        if (auction != null && auction.getId() > 0) {
            auctions.put(auction.getId(), auction);
        }
    }

    public AuctionDetailDTO getAuctionDetail(int auctionId) throws Exception {
        Auction auction = getAuctionModelById(auctionId);
        return mapToAuctionDetailDTO(auction);
    }

    public List<AuctionSummaryDTO> getAllAuctions() {
        List<AuctionSummaryDTO> auctionDTOs = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            auctionDTOs.add(mapToAuctionSummaryDTO(auction));
        }
        return auctionDTOs;
    }

    public void closeAuction(int requesterId, int auctionId) throws Exception {
        Auction auction;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
                }

                User requester = userRepository.getUserById(conn, requesterId);
                if (requester == null) {
                    throw new ResourceNotFoundException("Người dùng", requesterId);
                }

                boolean isOwner = auction.getItem().getOwner().getId() == requesterId;
                boolean isAdmin = requester.isAdmin();
                if (!isOwner && !isAdmin) {
                    throw new AuthorizationException("Bạn không có quyền đóng phòng đấu giá này!");
                }

                if (auction.getStatus() == AuctionStatus.FINISHED) {
                    throw new AuctionAppException("Phòng đấu giá này đã được đóng từ trước!");
                }

                auction.close();
                auctionRepository.updateAuction(conn, auction);

                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        updateCachedAuction(auction);
        notifyAuctionClosed(auction);
    }

    private void notifyAuctionClosed(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionClosed(auction);
        }
    }

    public AuctionSummaryDTO mapToAuctionSummaryDTO(Auction auction) {
        return new AuctionSummaryDTO(
                auction.getId(),
                auction.getItem().getName(),
                auction.getItem().getItemType(),
                auction.getCurrentPrice(),
                auction.getEndTime(),
                auction.getStatus()
        );
    }

    public AuctionDetailDTO mapToAuctionDetailDTO(Auction auction) throws Exception {
        User owner = UserService.getInstance().getUserById(auction.getItem().getOwner().getId());
        ItemDTO itemDto = ItemService.getInstance().mapToItemDTO(auction.getItem());

        Integer lastBidderId = null;
        String lastBidderUsername = null;

        User lastBidder = auction.getLastBidder();
        if (lastBidder != null) {
            lastBidderId = lastBidder.getId();
            User fetchedBidder = UserService.getInstance().getUserById(lastBidderId);
            lastBidderUsername = fetchedBidder != null ? fetchedBidder.getUsername() : "Unknown";
        }

        return new AuctionDetailDTO(
                auction.getId(),
                owner.getId(),
                owner.getUsername(),
                itemDto,
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getBidStep(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus(),
                lastBidderId,
                lastBidderUsername
        );
    }
}
