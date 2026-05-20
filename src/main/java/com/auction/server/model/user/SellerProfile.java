package com.auction.server.model.user;

import com.auction.server.model.item.Item;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the seller profile containing a list of items for auction.
 */
public class SellerProfile implements Serializable {
    @Serial
    private static final long serialVersionUID = 8112310403512811443L;
    private final List<Item> itemsList = new ArrayList<>();

    /**
     * Adds an item to the seller's items list if the item is not null.
     *
     * @param item the item to add
     */
    public void addItem(Item item) {
        if (item != null) {
            itemsList.add(item);
        }
    }

    /**
     * Returns a defensive copy of the items list.
     *
     * @return a new ArrayList containing all items
     */
    public List<Item> getItemsList() {
        return new ArrayList<>(itemsList);
    }
}
