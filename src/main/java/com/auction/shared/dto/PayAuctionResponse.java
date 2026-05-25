package com.auction.shared.dto;

import java.io.Serial;
import java.io.Serializable;

public class PayAuctionResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final double paidAmount;
    private final double newBalance;
    private final double newUnpaidWinningAmount;
    private final double newAvailableBalance;
    private final String message;

    public PayAuctionResponse(int auctionId, double paidAmount, double newBalance,
                              double newUnpaidWinningAmount, double newAvailableBalance,
                              String message) {
        this.auctionId = auctionId;
        this.paidAmount = paidAmount;
        this.newBalance = newBalance;
        this.newUnpaidWinningAmount = newUnpaidWinningAmount;
        this.newAvailableBalance = newAvailableBalance;
        this.message = message;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public double getNewUnpaidWinningAmount() {
        return newUnpaidWinningAmount;
    }

    public double getNewAvailableBalance() {
        return newAvailableBalance;
    }

    public String getMessage() {
        return message;
    }
}
