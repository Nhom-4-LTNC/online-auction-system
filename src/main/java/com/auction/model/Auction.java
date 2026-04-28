package com.auction.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 6720930536578062003L;
    private int auctionId;
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double startPrice;
    private double currentPrice;
    private Bidder lastBidder;
    private final double bidStep;
    private AuctionStatus status;
    public Auction(int id, Item item, double bidStep, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startPrice = item.getStartPrice();
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public synchronized boolean placeBid(Bidder bidder, double amount) {
        LocalDateTime now = LocalDateTime.now();
        if (status == AuctionStatus.OPENED && now.isBefore(endTime) && amount >= currentPrice + bidStep) {
            currentPrice = amount;
            lastBidder = bidder;
            return true;
        }
        return false;
    }
    //STATUS UPDATE
    public void updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) status = AuctionStatus.INITIALIZED;
        else if (now.isAfter(endTime)) status = AuctionStatus.CLOSED;
        else status = AuctionStatus.OPENED;
    }
    //GET WINNER
    public Bidder getWinner() {
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.CLOSED;
            return lastBidder;
        }
        return null;
    }

    //GETTER, SETTER
    public Bidder getHighestBidder() {
        return lastBidder;
    }

    public void setLastBidder(Bidder lastBidder) {
        this.lastBidder = lastBidder;
    }

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

    public AuctionStatus getStatus() { return status; }
}
