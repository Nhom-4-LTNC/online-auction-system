package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.item.ItemType;
import com.auction.model.user.User;
import com.auction.repository.AuctionRepository;

import java.util.List;
import java.util.Map;

public class AuctionService {

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();

    public List<Auction> getAllAuctions() throws Exception {
        return auctionRepository.getAllAuctions();
    }

    public Auction getAuctionById(int id) throws Exception {
        Auction auction = auctionRepository.getAuctionById(id);
        if (auction == null) {
            throw new Exception("Auction not found with ID: " + id);
        }
        return auction;
    }

    public void createAuction(User seller, ItemType type, Map<String, Object> itemData,
                             double bidStep, long startTimeMillis, long endTimeMillis) throws Exception {
        // TODO: Implement auction creation logic
        // This would involve creating an Item first, then creating an Auction
        throw new Exception("Auction creation not implemented yet");
    }
}
