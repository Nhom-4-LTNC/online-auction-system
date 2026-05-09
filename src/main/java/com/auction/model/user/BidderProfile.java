package com.auction.model.user;


import java.io.Serial;
import java.io.Serializable;

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
    public synchronized boolean canAfford(double amount) {
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
