package com.auction.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    @Serial
    private static final long serialVersionUID = -7854589223891717708L;

    private final int auctionId;
    private final int bidderId;
    private final String bidderName;
    private final double amount;
    private final long timestamp;

    public BidTransaction(int auctionId, int bidderId, String bidderName, double amount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public BidTransaction(int id, int auctionId, int bidderId, String bidderName, double amount,
                          long timestamp) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getAuctionId() { return auctionId; }
    public int getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("BidTransaction[ID: %d, Auction: %d, Bidder: %s, Amount: %.2f, Time: %s]",
                getId(), auctionId, bidderName, amount, timestamp);
    }
}
