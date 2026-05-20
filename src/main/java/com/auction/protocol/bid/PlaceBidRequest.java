package com.auction.protocol.bid;

import java.io.Serial;
import java.io.Serializable;

public class PlaceBidRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final double amount;

    public PlaceBidRequest(int auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public double getAmount() {
        return amount;
    }
}
