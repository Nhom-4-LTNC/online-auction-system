package com.auction.server.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Art;
import com.auction.server.model.item.Electronics;
import com.auction.server.model.item.Item;
import com.auction.server.model.item.Vehicle;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.VehicleDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.exception.AuthorizationException;
import com.auction.shared.exception.ResourceNotFoundException;
import com.auction.shared.exception.ValidationException;
import com.auction.shared.util.ItemFactory;

public class AuctionService {

    private static volatile AuctionService instance;

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private AuctionService() {
        // Full auction graphs are loaded on demand. List screens use summary queries.
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
        return createAuction(sellerId, itemDto, bidStep, System.currentTimeMillis(), endTimeMillis);
    }

    public synchronized Auction createAuction(int sellerId, ItemDTO itemDto, double bidStep,
                                              long startTimeMillis, long endTimeMillis) throws Exception {
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
            auction = auctionRepository.getAuctionById(auctionId);
            if (auction != null) {
                updateCachedAuction(auction);
                return auction;
            }
            throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
        }
        return auction;
    }

    public void updateCachedAuction(Auction auction) {
        if (auction != null && auction.getId() > 0) {
            auctions.put(auction.getId(), auction);
        }
    }

    private boolean auctionStateChanged(AuctionStatus oldStatus, Integer oldWinnerId, Auction auction) {
        return oldStatus != auction.getStatus()
                || !java.util.Objects.equals(oldWinnerId, userIdOrNull(auction.getWinner()));
    }

    private Integer userIdOrNull(User user) {
        return user != null ? user.getId() : null;
    }

    public AuctionDetailDTO getAuctionDetail(int auctionId) throws Exception {
        Auction auction = refreshAndPersistIfChanged(auctionId);
        return mapToAuctionDetailDTO(auction);
    }

    public List<AuctionSummaryDTO> getAuctionSummariesByType(ItemType itemType) throws Exception {
        if (itemType == null) {
            throw new AuctionAppException("Loại sản phẩm không hợp lệ.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            auctionRepository.finalizeExpiredAuctionsForRead(conn, System.currentTimeMillis());
            return auctionRepository.getAuctionSummariesByType(conn, itemType);
        }
    }
    public List<AuctionSummaryDTO> getAllAuctions() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            auctionRepository.finalizeExpiredAuctionsForRead(conn, System.currentTimeMillis());
            return auctionRepository.getAllAuctionSummaries(conn);
        }
    }

    public List<AuctionSummaryDTO> getAuctionsCreatedByUser(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            auctionRepository.finalizeExpiredAuctionsForRead(conn, System.currentTimeMillis());
            return auctionRepository.findSummariesBySellerId(conn, userId);
        }
    }

    public List<AuctionSummaryDTO> getAuctionsParticipatedByUser(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            auctionRepository.finalizeExpiredAuctionsForRead(conn, System.currentTimeMillis());
            return auctionRepository.findSummariesParticipatedByBidderId(conn, userId);
        }
    }

    public List<AuctionSummaryDTO> getAuctionsWonByUser(int userId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            auctionRepository.finalizeExpiredAuctionsForRead(conn, System.currentTimeMillis());
            return auctionRepository.findSummariesWonByUserId(conn, userId);
        }
    }

    public List<AuctionSummaryDTO> getAllAuctionSummaries() {

        try {
            // Gọi sang hàm Repository tối ưu vừa thêm ở Bước 1
            return auctionRepository.getAllAuctionSummaries();
        } catch (Exception e) {
            System.err.println("[AuctionService] Cannot load auction summaries: " + e.getMessage());
            return new ArrayList<>(); // Trả về danh sách rỗng để tránh lỗi Null giao diện
        }
    }

    public Auction refreshAndPersistIfChanged(int auctionId) throws Exception {
        Auction auction;
        boolean changed;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
                }

                AuctionStatus oldStatus = auction.getStatus();
                Integer oldWinnerId = userIdOrNull(auction.getWinner());
                auction.refreshStatus(System.currentTimeMillis());
                changed = auctionStateChanged(oldStatus, oldWinnerId, auction);

                if (changed) {
                    auctionRepository.updateAuction(conn, auction);
                }

                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        updateCachedAuction(auction);
        if (changed && auction.getStatus() == AuctionStatus.FINISHED) {
            notifyAuctionClosed(auction);
        }
        return auction;
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

                if (auction.getStatus() == AuctionStatus.FINISHED
                        || auction.getStatus() == AuctionStatus.PAID
                        || auction.getStatus() == AuctionStatus.CANCELED) {
                    throw new AuctionAppException("Phòng đấu giá này đã được đóng từ trước!");
                }

                auction.close();
                auctionRepository.updateAuction(conn, auction);

                Auction reloaded = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (reloaded != null) {
                    auction = reloaded;
                }

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

    public AuctionDetailDTO updateAuctionItem(int requesterId, int auctionId, ItemDTO itemDto,
                                              long startTimeMillis, long endTimeMillis) throws Exception {
        Auction auction;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
                }

                requireOwnerOfOpenAuction(requesterId, auction);
                applyAuctionItemUpdate(auction, itemDto, startTimeMillis, endTimeMillis);

                itemRepository.updateItem(conn, auction.getItem());
                auctionRepository.updateAuctionSchedule(conn, auction);

                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        updateCachedAuction(auction);
        return mapToAuctionDetailDTO(auction);
    }

    public Auction cancelAuction(int requesterId, int auctionId) throws Exception {
        Auction auction;
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
                }

                requireCancelPermission(requesterId, auction, conn);
                auction.setStatus(AuctionStatus.CANCELED);
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
        return auction;
    }

    private void requireOwnerOfOpenAuction(int requesterId, Auction auction) throws AuctionAppException {
        auction.refreshStatus(System.currentTimeMillis());

        int ownerId = auction.getItem().getOwner().getId();
        if (ownerId != requesterId) {
            throw new AuthorizationException("Chỉ chủ phiên đấu giá mới được thực hiện thao tác này.");
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new AuctionAppException("Chỉ có thể sửa hoặc hủy phiên đấu giá khi trạng thái là OPEN.");
        }
    }

    private void requireCancelPermission(int requesterId, Auction auction, Connection conn) throws Exception {
        auction.refreshStatus(System.currentTimeMillis());

        User requester = userRepository.getUserById(conn, requesterId);
        if (requester == null) {
            throw new ResourceNotFoundException("Người dùng", requesterId);
        }
        if (requester.isAdmin()) {
            return;
        }

        int ownerId = auction.getItem().getOwner().getId();
        if (ownerId != requesterId) {
            throw new AuthorizationException("Chỉ chủ phiên đấu giá hoặc Admin mới được hủy phiên đấu giá.");
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new AuctionAppException("Chủ phiên chỉ có thể hủy khi phiên đấu giá đang ở trạng thái OPEN.");
        }
    }

    private void applyAuctionItemUpdate(Auction auction, ItemDTO itemDto,
                                        long startTimeMillis, long endTimeMillis) throws Exception {
        if (itemDto == null) {
            throw new ValidationException("Thông tin sản phẩm cập nhật không hợp lệ.");
        }

        Item item = auction.getItem();
        if (itemDto.getType() != item.getItemType()) {
            throw new ValidationException("Không được thay đổi loại sản phẩm của phiên đấu giá.");
        }

        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());

        String newImageUrl = ItemService.getInstance().saveImage(
                itemDto.getImageData(),
                itemDto.getImageFileName()
        );
        if (newImageUrl != null) {
            item.setImageUrl(newImageUrl);
        }

        applySubtypeFields(item, itemDto);
        AuctionValidator.validateItem(item);

        long newStartTime = startTimeMillis > 0 ? startTimeMillis : auction.getStartTime();
        long newEndTime = endTimeMillis > 0 ? endTimeMillis : auction.getEndTime();
        validateOpenAuctionSchedule(newStartTime, newEndTime);

        auction.setStartTime(newStartTime);
        auction.setEndTime(newEndTime);
        auction.setStatus(AuctionStatus.OPEN);
    }

    private void applySubtypeFields(Item item, ItemDTO itemDto) throws ValidationException {
        if (item instanceof Art art && itemDto instanceof ArtDTO artDTO) {
            art.setArtist(artDTO.getArtist());
            art.setYearCreated(artDTO.getYearCreated());
            return;
        }
        if (item instanceof Electronics electronics && itemDto instanceof ElectronicsDTO electronicsDTO) {
            electronics.setBrand(electronicsDTO.getBrand());
            electronics.setWarrantyMonths(electronicsDTO.getWarrantyMonths());
            return;
        }
        if (item instanceof Vehicle vehicle && itemDto instanceof VehicleDTO vehicleDTO) {
            vehicle.setBrand(vehicleDTO.getBrand());
            vehicle.setVin(vehicleDTO.getVin());
            vehicle.setMileage(vehicleDTO.getMileage());
            return;
        }

        throw new ValidationException("Dữ liệu sản phẩm cập nhật không khớp với loại sản phẩm hiện tại.");
    }

    private void validateOpenAuctionSchedule(long startTimeMillis, long endTimeMillis) throws ValidationException {
        long now = System.currentTimeMillis();
        if (startTimeMillis <= now) {
            throw new ValidationException("Thời gian bắt đầu phải lớn hơn thời gian hiện tại để phiên vẫn ở trạng thái OPEN.");
        }
        if (endTimeMillis <= startTimeMillis) {
            throw new ValidationException("Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
        }
    }

    private void notifyAuctionClosed(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionClosed(auction);
        }
    }

    public AuctionSummaryDTO mapToAuctionSummaryDTO(Auction auction) {
        return new AuctionSummaryDTO(
                auction.getId(),
                auction.getItem().getId(),
                auction.getItem().getName(),
                auction.getItem().getItemType(),
                auction.getCurrentPrice(),
                auction.getEndTime(),
                auction.getStatus(),
                auction.getWinner() == null ? null : auction.getWinner().getId()
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
