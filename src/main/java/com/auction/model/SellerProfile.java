package com.auction.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SellerProfile implements Serializable {
    @Serial
    private static final long serialVersionUID = 8112310403512811443L;
    private final List<Item> itemslist = new ArrayList<>();
    public void addItem(Item item) {
        if (item != null)
            itemslist.add(item);
    }
    public List<Item> getItemslist() {
        return new ArrayList<>(itemslist);
    }
}
