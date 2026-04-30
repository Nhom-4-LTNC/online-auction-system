package com.auction.model.user;

import com.auction.model.item.Item;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SellerProfile implements Serializable {
    @Serial
    private static final long serialVersionUID = 8112310403512811443L;
    List<Item> itemslist = new ArrayList<>();
    public void addItem(Item item) {
        if (item != null)
            itemslist.add(item);
    }
    public List<Item> getItemslist() {
        return new ArrayList<>(itemslist);
    }
}
