package com.auction.shared.protocol.finance;

import java.io.Serial;
import java.io.Serializable;

public class AddBalanceRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double balance;
    public AddBalanceRequest(double balance) {
        this.balance = balance;
    }
    public double getBalance() {
        return balance;
    }
    @Override
    public String toString() {
        return "AddBalanceRequest{" +
                "balance=" + balance +
                '}';
    }
}
