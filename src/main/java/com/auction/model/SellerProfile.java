package com.auction.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class SellerProfile{
    List<Item> itemslist = new ArrayList<>();
    public void addItem(Item item) {
        if (item != null)
            itemslist.add(item);
    }
    public List<Item> getItemslist() {
        return new ArrayList<>(itemslist);
    }
}
