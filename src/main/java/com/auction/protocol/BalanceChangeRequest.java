package com.auction.protocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * Client -> Server: request to change user's balance
 */
public class BalanceChangeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final double amount;
    private final boolean isIncrement; // true = add/subtract, false = set

    public BalanceChangeRequest(int userId, double amount, boolean isIncrement) {
        this.userId = userId;
        this.amount = amount;
        this.isIncrement = isIncrement;
    }

    public int getUserId() { return userId; }
    public double getAmount() { return amount; }
    public boolean isIncrement() { return isIncrement; }
}

