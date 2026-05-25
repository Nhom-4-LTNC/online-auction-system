package com.auction.shared.dto;

import java.io.Serial;
import java.io.Serializable;

public class BalanceResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double balance;
    private final double unpaidWinningAmount;
    private final double availableBalance;
    private final String message;

    public BalanceResponse(double balance, double unpaidWinningAmount,
                           double availableBalance, String message) {
        this.balance = balance;
        this.unpaidWinningAmount = unpaidWinningAmount;
        this.availableBalance = availableBalance;
        this.message = message;
    }

    public double getBalance() {
        return balance;
    }

    public double getUnpaidWinningAmount() {
        return unpaidWinningAmount;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public String getMessage() {
        return message;
    }
}
