package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.item.ItemType;

import java.util.List;
import java.util.Map;

public interface ItemService {
    Item createItem(ItemType type, Map <String, Object> itemData) throws IllegalArgumentException;
    List <Item> getItemBySeller(int sellerId) throws Exception;

}
