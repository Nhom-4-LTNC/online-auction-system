package com.auction.server.service;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.Bid;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BidService {
    private static final int RECENT_BID_LIMIT = 20;

    private static volatile BidService instance;

    private final BidRepository bidRepository = BidRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final UserService userService = UserService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final WalletService walletService = WalletService.getInstance();

    private BidService() {}

    public static BidService getInstance() {
        if (instance == null) {
            synchronized (BidService.class) {
                if (instance == null) instance = new BidService();
            }
        }
        return instance;
    }

    public PlaceBidResult placeBid(int bidderId, int auctionId, double amount) throws Exception {
        if (bidderId <= 0) {
            throw new AuctionAppException("Bạn cần đăng nhập để đặt giá!");
        }

        Auction auction;
        Auction expiredAuction = null;
        BidDTO latestBid = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
                }

                User bidder = userRepository.getUserById(conn, bidderId);
                if (bidder == null) {
                    throw new ResourceNotFoundException("Người đặt giá", bidderId);
                }

                AuctionStatus oldStatus = auction.getStatus();
                Integer oldWinnerId = userIdOrNull(auction.getWinner());
                auction.refreshStatus(System.currentTimeMillis());

                if (!auction.isBiddable()) {
                    if (auctionStateChanged(oldStatus, oldWinnerId, auction)) {
                        auctionRepository.updateAuction(conn, auction);
                    }
                    conn.commit();
                    expiredAuction = auction;
                } else {
                    double availableForThisBid = walletService.getAvailableBalanceForBid(conn, bidderId, auctionId);
                    if (availableForThisBid < amount) {
                        throw new InsufficientFundsException("Số dư khả dụng không đủ để đặt giá.");
                    }

                    Bid bid = auction.placeBid(bidder, amount);
                    bidRepository.save(conn, bid);
                    latestBid = new BidDTO(
                            bid.getId(),
                            bid.getAuctionId(),
                            bidder.getId(),
                            bidder.getUsername(),
                            bid.getAmount(),
                            bid.getTimestamp()
                    );
                    auctionRepository.updateAuction(conn, auction);
                    conn.commit();
                }
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        auctionService.updateCachedAuction(auction);
        if (expiredAuction != null) {
            throw new AuctionClosedException(auctionId);
        }
        return new PlaceBidResult(auction, latestBid);
    }

    public record PlaceBidResult(Auction updatedAuction, BidDTO latestBid) {}

    private boolean auctionStateChanged(AuctionStatus oldStatus, Integer oldWinnerId, Auction auction) {
        return oldStatus != auction.getStatus()
                || !java.util.Objects.equals(oldWinnerId, userIdOrNull(auction.getWinner()));
    }

    private Integer userIdOrNull(User user) {
        return user != null ? user.getId() : null;
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[BidService] Cannot rollback placeBid transaction: "
                    + rollbackError.getMessage());
        }
    }

    private void restoreAutoCommitQuietly(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreError) {
            System.err.println("[BidService] Cannot restore autoCommit: "
                    + restoreError.getMessage());
        }
    }

    public List<Bid> getBidsByAuctionId(int auctionId) throws Exception {
        return bidRepository.findByAuctionId(auctionId);
    }

    public List<BidDTO> getBidHistoryByAuction(int auctionId) throws Exception {
        return bidRepository.getBidDTOsByAuctionId(auctionId);
    }

    public List<BidDTO> getRecentBidHistoryByAuction(int auctionId) throws Exception {
        return bidRepository.getRecentBidDTOsByAuctionId(auctionId, RECENT_BID_LIMIT);
    }

    public Bid getHighestBidByAuctionId(int auctionId) throws Exception {
        return bidRepository.findHighestBidByAuctionId(auctionId);
    }

    public List<Bid> getBidsByBidder(int bidderId) throws Exception {
        return bidRepository.findByBidderId(bidderId);
    }

    public List<BidDTO> getBidHistoryByBidder(int bidderId) throws Exception {
        return bidRepository.getBidDTOsByBidderId(bidderId);
    }

    public List<Bid> getBidsByBidderForRequester(int requesterId, int bidderId) throws Exception {
        User requester = userService.getUserById(requesterId);
        if (!requester.isAdmin() && requesterId != bidderId) {
            throw new AuthorizationException(
                    "Bạn không có quyền xem lịch sử bid của người dùng khác!"
            );
        }
        return getBidsByBidder(bidderId);
    }

    public List<BidDTO> getBidHistoryByBidderForRequester(int requesterId, int bidderId) throws Exception {
        User requester = userService.getUserById(requesterId);
        if (!requester.isAdmin() && requesterId != bidderId) {
            throw new AuthorizationException(
                    "Bạn không có quyền xem lịch sử bid của người dùng khác!"
            );
        }
        return getBidHistoryByBidder(bidderId);
    }

    public BidDTO mapToBidDTO(Bid bid) throws AuctionAppException {
        User bidder;
        try {
            bidder = userService.getUserById(bid.getBidderId());
        } catch (Exception e) {
            throw new AuctionAppException("Lỗi khi lấy thông tin người đặt giá");
        }

        String bidderUsername = bidder != null
                ? bidder.getUsername()
                : "Unknown";

        return new BidDTO(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bidderUsername,
                bid.getAmount(),
                bid.getTimestamp()
        );
    }

    public List<BidDTO> mapToBidDTOList(List<Bid> bids) throws AuctionAppException {
        return bids.stream()
                .map(bid -> {
                    try {
                        return mapToBidDTO(bid);
                    } catch (Exception e) {
                        throw new RuntimeException(new AuctionAppException("Không thể map Bid sang BidDTO"));
                    }
                })
                .toList();
    }
}
