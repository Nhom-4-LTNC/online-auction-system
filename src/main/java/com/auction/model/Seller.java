package com.auction.model;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    List<Item> itemslist = new ArrayList<>();
    public Seller(String username, String pwd, String email) {
        super(username, pwd, email);
    }
    public Seller(int id, String username, String pwd, String email) {
        super(id, username, pwd, email);
    }
    public Seller(Seller other) {
        super(other.getId(), other.getUsername(), other.getPwd(), other.getEmail());
    }
    public void addItem(Item item) {
        itemslist.add(item);
    }

    @Override
    public void displayInfo() {
        //TODO
    }
}
