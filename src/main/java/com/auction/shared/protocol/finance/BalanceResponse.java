package com.auction.shared.protocol.finance;

import java.io.Serial;
import java.io.Serializable;

public class BalanceResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double balance;
    private final String message;
    public BalanceResponse(double balance, String message) {
        this.balance = balance;
        this.message = message;
    }
    public double getBalance() {
        return balance;
    }
    public String getMessage() {
        return message;
    }
    @Override
    public String toString() {
        return String.format("BalanceResponse{balance=%.2f, message='%s'}", balance, message);
    }
}
