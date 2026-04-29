package com.auction.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager instance;
    private final Map <Integer, Auction> auctions;

    private AuctionManager() {
        /*
            ConcurrentHashMap an toàn trong môi trường đa luồng
         */
        auctions = new ConcurrentHashMap<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
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
    public List <Auction> getAllAuction() {
        return new ArrayList<>(auctions.values());
    }

    public boolean processBid(int auctionId, User user, double amount) {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            return auction.placeBid(user, amount);
        }
        return false;
    }
}
