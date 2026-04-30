package com.auction.model.user;

import com.auction.model.item.Item;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BidderProfile implements Serializable {
    @Serial
    private static final long serialVersionUID = 7652995670457990055L;
    private double balance;

    public synchronized boolean deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            return true;
        }
        return false;
    }
    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }
    public synchronized boolean withdraw(double amount) {
        if (canAfford(amount)) {
            this.balance -= amount;
            return true;
        }
        return false;
    }
    public double getBalance() {
        return balance;
    }

}
