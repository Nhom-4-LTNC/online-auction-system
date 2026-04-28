package com.auction.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    @Serial
    private static final long serialVersionUID = -5303235272624046213L;
    List<Item> itemslist = new ArrayList<>();
    public Seller(String username, String pwd, String email) {
        super(username, pwd, email);
        updateRole();
    }
    public Seller(int id, String username, String pwd, String email) {
        super(id, username, pwd, email);
        updateRole();
    }
    public void addItem(Item item) {
        if (item != null)
            itemslist.add(item);
    }

    public List<Item> getItemslist() {
        return new ArrayList<>(itemslist);
    }

    @Override
    public void updateRole() {
        this.role = Role.SELLER;
    }

    @Override
    public void displayInfo() {
        System.out.println(toString());
        System.out.println("Role: " + role);
    }
}
