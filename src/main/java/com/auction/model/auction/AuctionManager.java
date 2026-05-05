package com.auction.model.auction;

import com.auction.exception.InvalidBidException;
import com.auction.model.BidTransaction;
import com.auction.model.item.ItemType;
import com.auction.model.user.SellerProfile;
import com.auction.model.user.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map <Integer, Auction> auctions;

    private final List <AuctionObserver> observers = new CopyOnWriteArrayList<>();

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
        SellerProfile sellerProfile = seller.getSellerProfile();

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
