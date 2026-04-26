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
        balance = 0.0;
    }
    public Bidder(int id, String username, String pwd, String email) {
        super(id, username, pwd, email);
        balance = 0.0;
    }
    public void deposit(double amount) {
        balance += amount;
    }
    public void withdraw(double amount) {
        balance -= amount;
    }
    public double getBalance() {
        return balance;
    }

    @Override
    public void displayInfo() {
        System.out.println(this);
        System.out.println();
    }
    public List<Item> getBidList() {
        return bidList;
    }
}
