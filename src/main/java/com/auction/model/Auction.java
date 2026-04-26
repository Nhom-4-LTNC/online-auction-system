package com.auction.model;

import java.time.LocalDateTime;

public class Auction {
    private int auctionId;
    private Item item;
    private Seller seller;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double startPrice;
    private double currentPrice;
    private Bidder highestBidder;
    private double bidStep;
    private AuctionStatus status;
    public Auction(Seller seller, Item item, double startPrice, double bidStep, LocalDateTime startTime, LocalDateTime endTime) {
        this.seller = seller;

    }
    public boolean placeBid(Bidder bidder, double amount) {
        if (amount >= currentPrice + bidStep) {
            currentPrice = amount;
            highestBidder = bidder;
            return true;
        }
        return false;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(Bidder highestBidder) {
        this.highestBidder = highestBidder;
    }
}
