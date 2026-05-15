package com.auction.protocol.bid;

import java.io.Serial;
import java.io.Serializable;

public class BidRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int userId;
    private final double amount;

    public BidRequest(int auctionId, int user, double amount) {
        this.auctionId = auctionId;
        this.userId = user;
        this.amount = amount;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }
}
