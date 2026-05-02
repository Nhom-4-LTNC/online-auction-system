package com.auction.model.auction;

import com.auction.exception.InvalidBidException;
import com.auction.model.user.User;

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

    public void processBid(int auctionId, User user, double amount) throws InvalidBidException, Exception {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.placeBid(user, amount);
        } else {
            throw new Exception("Khong tim thay phien dau gia");
        }
        
    }
}
