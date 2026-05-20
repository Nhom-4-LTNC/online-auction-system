package com.auction.service;

import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.ItemDTO;
import com.auction.exception.AuctionAppException;
import com.auction.exception.AuthorizationException;
import com.auction.exception.ResourceNotFoundException;
import com.auction.model.Bid;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionObserver;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.Role;
import com.auction.model.user.User;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Service xử lý nghiệp vụ đấu giá.
 * Áp dụng Cache In-memory để tăng tốc độ truy xuất, tự động đồng bộ xuống DB.
 */
public class AuctionService {

    private static volatile AuctionService instance;

    /** Cache in-memory: key = auction ID, truy cập O(1). */
    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    /** Danh sách observer nhận thông báo khi trạng thái phiên thay đổi (Dùng cho Realtime). */
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final BidRepository bidRepository = BidRepository.getInstance();

    private AuctionService() {
        try {
            // Khởi động server: Tải toàn bộ phòng đấu giá từ DB lên Cache
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

    // ==========================================
    // NHÓM QUẢN LÝ PHÒNG ĐẤU GIÁ (AUCTION)
    // ==========================================

    /**
     * Tạo phòng đấu giá mới.
     */
    public synchronized Auction createAuction(int sellerId, ItemDTO itemDto, double bidStep, long endTimeMillis) throws Exception {
        // 1. Lấy thông tin người bán
        User seller = UserService.getInstance().getUserById(sellerId);
        if (seller == null) {
            throw new ResourceNotFoundException("Người dùng (Người bán)", sellerId);
        }

        long startTimeMillis = System.currentTimeMillis();

        // 2. Validate dữ liệu
        AuctionValidator.validateAuctionParams(startTimeMillis, endTimeMillis, bidStep);

        // 3. Tạo Item và lưu DB để lấy ID tự tăng
        Item newItem = ItemService.getInstance().createItem(seller, itemDto);

        // Thêm vào profile của seller (nếu bạn có dùng tới)
        if (seller.getSellerProfile() != null) {
            seller.getSellerProfile().addItem(newItem);
        }

        // 4. Tạo Auction và lưu DB
        Auction newAuction = new Auction(newItem, bidStep, startTimeMillis, endTimeMillis);
        auctionRepository.addAuction(newAuction);

        // 5. Cập nhật Cache in-memory
        auctions.put(newAuction.getId(), newAuction);

        return newAuction;
    }
    public Auction getAuctionModelById(int auctionId) throws Exception {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
        }

        return auction;
    }
    /**
     * Lấy chi tiết 1 phòng đấu giá (Đã sửa lỗi NullPointerException khi chưa có ai Bid).
     */
    public AuctionDetailDTO getAuctionDetail(int auctionId) throws Exception {
        Auction auction = getAuctionModelById(auctionId);
        return mapToAuctionDetailDTO(auction);
    }

    /**
     * Lấy danh sách tóm tắt toàn bộ phòng đấu giá.
     */
    public List<AuctionSummaryDTO> getAllAuctions() {
        List<AuctionSummaryDTO> auctionDTOs = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            auctionDTOs.add(mapToAuctionSummaryDTO(auction));
        }
        return auctionDTOs;
    }

    /**
     * Đóng phòng đấu giá (Kết thúc sớm).
     * Chỉ Chủ phòng hoặc Admin mới có quyền thực hiện.
     */
    public void closeAuction(int requesterId, int auctionId) throws Exception {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new ResourceNotFoundException("Phòng đấu giá", auctionId);
        }

        User requester = UserService.getInstance().getUserById(requesterId);
        boolean isOwner = auction.getItem().getOwnerId() == requesterId;
        boolean isAdmin = requester.hasRole(Role.ADMIN);

        // Kiểm tra quyền
        if (!isOwner && !isAdmin) {
            throw new AuthorizationException("Bạn không có quyền đóng phòng đấu giá này!");
        }

        if (auction.getStatus() == AuctionStatus.FINISHED) {
            throw new AuctionAppException("Phòng đấu giá này đã được đóng từ trước!");
        }

        // Thực hiện đóng
        auction.setStatus(AuctionStatus.FINISHED);
        auctionRepository.updateAuction(auction); // Cập nhật DB
    }

    // ==========================================
    // NHÓM MAPPER (CHUYỂN ĐỔI ENTITY -> DTO)
    // ==========================================

    public AuctionSummaryDTO mapToAuctionSummaryDTO(Auction auction) {
        return new AuctionSummaryDTO(
                auction.getId(),
                auction.getItem().getName(),
                auction.getItem().getCategory(),
                auction.getCurrentPrice(),
                auction.getEndTime(),
                auction.getStatus()
        );
    }

    public AuctionDetailDTO mapToAuctionDetailDTO(Auction auction) throws Exception {
        User owner = UserService.getInstance().getUserById(auction.getItem().getOwnerId());
        ItemDTO itemDto = ItemService.getInstance().mapToItemDTO(auction.getItem());

        // FIX LỖI NPE: Kiểm tra xem đã có ai bid chưa
        Integer lastBidderId = null;
        String lastBidderUsername = null;

        User lastBidder = auction.getLastBidder();
        if (lastBidder != null) {
            lastBidderId = lastBidder.getId();
            // Fetch username đầy đủ từ DB đề phòng cache User bị thiếu data
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

    // ==========================================
    // NHÓM OBSERVER (REALTIME)
    // ==========================================

    public void addObserver(AuctionObserver observer) { observers.add(observer); }
    public void removeObserver(AuctionObserver observer) { observers.remove(observer); }
}