package com.auction.service;

import com.auction.exception.InvalidBidException;
import com.auction.model.BidTransaction;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionObserver;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.model.item.ItemType;
import com.auction.model.user.User;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * AuctionService — tầng Service cho Đấu Giá
 *
 * Trách nhiệm (thay thế cho AuctionManager cũ):
 *   1. Quản lý danh sách phiên đấu giá trong bộ nhớ (truy cập nhanh)
 *   2. Xử lý nghiệp vụ: tạo phiên, đặt giá, thông báo Observer
 *   3. Lưu dữ liệu xuống file qua Repository
 *
 * Luồng dữ liệu: Controller → AuctionService → AuctionRepository → auction.dat
 */
public class AuctionService {

    // Singleton: cả ứng dụng chỉ dùng một instance
    private static volatile AuctionService instance;

    // Lưu phiên đấu giá trong bộ nhớ để truy cập nhanh (key = auction ID)
    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    // Danh sách observer nhận thông báo khi có bid mới (ví dụ: cập nhật UI)
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Bộ đếm ID tự động tăng, an toàn khi nhiều luồng chạy đồng thời
    private final AtomicInteger auctionIdCounter = new AtomicInteger(1);
    private final AtomicInteger itemIdCounter    = new AtomicInteger(1);

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final ItemRepository    itemRepository    = ItemRepository.getInstance();

    private AuctionService() {
        // Khi khởi động app: load dữ liệu từ file vào bộ nhớ
        for (Auction auction : auctionRepository.getAllAuctions()) {
            auctions.put(auction.getId(), auction);
            // Đảm bảo counter không bị trùng ID với dữ liệu đã lưu
            if (auction.getId() >= auctionIdCounter.get()) {
                auctionIdCounter.set(auction.getId() + 1);
            }
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


    public synchronized Auction createAuction(User seller, ItemType type,
                                              Map<String, Object> itemData,
                                              double bidStep,
                                              long startTimeMillis,
                                              long endTimeMillis) throws Exception {
        long now = System.currentTimeMillis();
        if (startTimeMillis <= 0) startTimeMillis = now;

        if (endTimeMillis <= startTimeMillis || endTimeMillis <= now)
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải lớn hơn thời gian bắt đầu và thời điểm hiện tại!");

        if (bidStep <= 0)
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0!");

        // Gắn thông tin bắt buộc vào map trước khi tạo Item
        itemData.put("owner", seller);
        itemData.put("id", itemIdCounter.getAndIncrement());

        // Tạo Item qua Factory (phân loại: Electronics / Art / Vehicle)
        Item newItem = ItemFactory.createItem(type, itemData);
        if (newItem == null || !newItem.isValid())
            throw new Exception("Thông tin sản phẩm không hợp lệ!");

        // Tạo Auction với ID mới
        int newId = auctionIdCounter.getAndIncrement();
        Auction newAuction = new Auction(newId, newItem, bidStep, startTimeMillis, endTimeMillis);

        // Thêm item vào danh sách của người bán
        seller.getSellerProfile().addItem(newItem);

        // Lưu vào bộ nhớ (để truy cập nhanh trong phiên làm việc)
        auctions.put(newId, newAuction);

        // Lưu vào file (để không mất dữ liệu khi tắt app)
        itemRepository.addItem(newItem);
        auctionRepository.addAuction(newAuction);

        return newAuction;
    }

    public synchronized void placeBid(int auctionId, User bidder, double amount)
            throws InvalidBidException, Exception {

        Auction auction = auctions.get(auctionId);
        if (auction == null)
            throw new Exception("Không tìm thấy phiên đấu giá với ID: " + auctionId);

        BidTransaction txn = auction.placeBid(bidder, amount);

        auctionRepository.updateAuction(auction);

        for (AuctionObserver observer : observers) {
            observer.onNewBidPlace(txn);
        }
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public Auction getAuctionById(int id) throws Exception {
        Auction auction = auctions.get(id);
        if (auction == null)
            throw new Exception("Không tìm thấy phiên đấu giá với ID: " + id);
        return auction;
    }

    public void addObserver(AuctionObserver observer)    { observers.add(observer); }
    public void removeObserver(AuctionObserver observer) { observers.remove(observer); }
}
