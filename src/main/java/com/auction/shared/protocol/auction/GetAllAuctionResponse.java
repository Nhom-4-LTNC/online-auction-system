package com.auction.shared.protocol.auction;

import com.auction.shared.dto.AuctionSummaryDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GetAllAuctionResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AuctionSummaryDTO> auctions;
    private final String message;
    public GetAllAuctionResponse(List<AuctionSummaryDTO> auctions, String message) {
        this.auctions = auctions;
        this.message = message;
    }

    public List<AuctionSummaryDTO> getAuctions() {
        return auctions;
    }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("GetAllAuctionResponse{auctions=%s, message='%s'}", auctions, message);
    }
}
