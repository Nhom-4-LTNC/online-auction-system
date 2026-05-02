package com.auction.network;

import com.auction.model.user.User;
import java.io.Serializable;

public class BidMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private int auctionId;
    private User user;
    private double amount;

    public BidMessage(int auctionId, User user, double amount) {
        this.auctionId = auctionId;
        this.user = user;
        this.amount = amount;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public User getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }
}
