package com.auction.shared.protocol.finance;

import java.io.Serial;
import java.io.Serializable;

public class AddBalanceRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double amount;

    public AddBalanceRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}
