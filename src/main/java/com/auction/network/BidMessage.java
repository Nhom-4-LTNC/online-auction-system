package com.auction.network;

import com.auction.model.user.User;

import java.io.Serial;
import java.io.Serializable;

public class BidMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int auctionId;
    private int userId;
    private double amount;

    public BidMessage(int auctionId, int user, double amount) {
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
