package com.auction.protocol;

import java.io.Serial;
import java.io.Serializable;

public class MessageData implements Serializable{
    @Serial
    private static final long serialVersionUID = 4988799950280796440L;
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
