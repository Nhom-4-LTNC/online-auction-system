package com.auction.model.auction;

import com.auction.exception.InvalidBidException;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.model.item.ItemType;
import com.auction.model.user.SellerProfile;
import com.auction.model.user.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map <Integer, Auction> auctions;

    private final List <AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final AtomicInteger auctionIdCounter = new AtomicInteger(1);
    private final AtomicInteger itemIdCounter = new AtomicInteger(1);
    private AuctionManager() {
        /*
            ConcurrentHashMap an toàn trong môi trường đa luồng
         */
        auctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public synchronized Auction createAuction(User seller, ItemType type, Map <String, Object> itemData,
                                 double bidStep, long startTimeMillis, long endTimeMillis) throws Exception {
        long currentTime = System.currentTimeMillis();
        if (startTimeMillis <= 0) {
            startTimeMillis = currentTime;
        }
        if (endTimeMillis <= startTimeMillis || endTimeMillis <= currentTime) {
            throw new IllegalArgumentException("Lỗi: Thời gian kết thúc phải lớn hơn thời gian bắt đầu và thời gian hiện tại!");
        }

        if (bidStep <= 0) {
            throw new IllegalArgumentException("Lỗi: Bước giá phải lớn hơn 0!");
        }

        SellerProfile sellerProfile = seller.getSellerProfile();

        itemData.put("owner", seller);
        itemData.put("id", itemIdCounter.getAndIncrement());

        Item newItem = ItemFactory.createItem(type, itemData);

        if (newItem == null || !newItem.isValid()) {
            throw new Exception("Lỗi: Thông tin sản phẩm không hợp lệ!");
        }

        int newAuctionId = auctionIdCounter.getAndIncrement();
        Auction newAuction = new Auction(newAuctionId, newItem, bidStep, startTimeMillis, endTimeMillis);

        sellerProfile.addItem(newItem);
        this.auctions.put(newAuction.getId(), newAuction);


        return newAuction;
    }
    public void addAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public Auction getAuction(int id) {
        return auctions.get(id);
    }
    /*
        getAllAuction(): Hiển thị danh sách các phiên đấu giá cho Client
     */
    public List <Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public void processBid(int auctionId, User user, double amount) throws InvalidBidException, Exception {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            BidTransaction newTxn = auction.placeBid(user, amount);

            for (AuctionObserver observer : observers) {
                observer.onNewBidPlace(newTxn);
            }
        } else {
            throw new Exception("Khong tim thay phien dau gia voi ID: " + auctionId);
        }
    }
}
