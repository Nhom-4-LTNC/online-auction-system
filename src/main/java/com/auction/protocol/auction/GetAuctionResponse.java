package com.auction.protocol.auction;

import com.auction.dto.AuctionDTO;
import com.auction.model.auction.Auction;

import java.util.List;

public class GetAuctionResponse {
    private final List<AuctionDTO> auctions;

    public GetAuctionResponse(List<AuctionDTO> auctions) {
        this.auctions = auctions;
    }

    public List<AuctionDTO> getAuctions() {
        return auctions;
    }

}
