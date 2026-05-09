package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.item.ItemType;
import com.auction.repository.ItemRepository;

import java.util.List;
import java.util.Map;

public class ItemService {

    private final ItemRepository itemRepository = ItemRepository.getInstance();

    public Item createItem(ItemType type, Map<String, Object> itemData) throws IllegalArgumentException {
        // TODO: Implement item creation based on type
        // This would create different item types (Vehicle, Electronics, Art) based on ItemType
        throw new IllegalArgumentException("Item creation not implemented yet");
    }

    public List<Item> getItemBySeller(int sellerId) throws Exception {
        return itemRepository.getItemsBySeller(sellerId);
    }
}
