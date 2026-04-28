package com.auction.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    @Serial
    private static final long serialVersionUID = -972067732547664039L;
    private double balance;
    private List<Item> bidList = new ArrayList<>();

    public Bidder(String username, String pwd, String email) {
        super(username, pwd, email);
        updateRole();
        balance = 0.0;
    }
    public Bidder(int id, String username, String pwd, String email) {
        super(id, username, pwd, email);
        updateRole();
        balance = 0.0;
    }
    public void addBidItem(Item item) {
        if (!bidList.contains(item) && item != null)
            bidList.add(item);
    }
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

    @Override
    public void displayInfo() {
        System.out.println(super.toString());
        System.out.printf("Role: %s | balance: %.2f\n", role, balance);

    }

    @Override
    public void updateRole() {
        this.role = Role.BIDDER;
    }

    public List<Item> getBidList() {
        return new ArrayList<>(bidList);
    }
}
