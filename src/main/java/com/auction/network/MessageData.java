package com.auction.network;

import java.io.Serializable;

public class MessageData {
    private String userName;
    private double bidAmount;

    public MessageData(String userName, double bidAmount) {
        this.userName = userName;
        this.bidAmount = bidAmount;
    }

    @Override
    public String toString() {
        return userName+" vừa đặt giá: "+bidAmount;
    }
}
