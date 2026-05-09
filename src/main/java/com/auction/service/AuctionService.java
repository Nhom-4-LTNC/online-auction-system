package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.item.ItemType;
import com.auction.model.user.User;

import java.util.List;
import java.util.Map;

public interface AuctionService {
    List <Auction> getAllAuctions() throws Exception;
    Auction getAuctionById(int id) throws Exception;
    void createAuction(User seller, ItemType type, Map<String, Object> itemData,
                       double bidStep, long startTimeMillis, long endTimeMillis) throws Exception;
}
