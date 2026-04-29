package com.auction.model;

import java.io.Serial;
import java.time.LocalDateTime;

public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 6720930536578062003L;
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double startPrice;
    private double currentPrice;
    private User lastBidder;
    private final double bidStep;
    private AuctionStatus status;
    public Auction(int id, Item item, double bidStep, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startPrice = item.getStartPrice();
        this.currentPrice = startPrice;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public synchronized boolean placeBid(User user, double amount) {
        LocalDateTime now = LocalDateTime.now();
        if (status == AuctionStatus.CLOSED || now.isAfter(endTime)) return false;
        if (!user.hasRole(Role.BIDDER) || user.getBidderProfile() == null) return false;
        if (amount < currentPrice + bidStep) return false;

        BidderProfile profile = user.getBidderProfile();

        if (!profile.canAfford(amount)) return false;

        this.currentPrice = amount;
        this.lastBidder = user;
        profile.addBidItem(this.item);

        return true;
    }
    //STATUS UPDATE
    public AuctionStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return AuctionStatus.INITIALIZED;
        else if (now.isAfter(endTime)) return AuctionStatus.CLOSED;
        return AuctionStatus.OPENED;
    }
    //GET WINNER
    public User getWinner() {
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.CLOSED;
            return lastBidder;
        }
        return null;
    }

    //GETTER, SETTER
    public User getHighestBidder() {
        return lastBidder;
    }
    public void setLastBidder(User user) { lastBidder = user; }
    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

}
