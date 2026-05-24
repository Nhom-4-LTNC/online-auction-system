package com.auction.shared.protocol.auction;

import com.auction.shared.dto.AuctionSummaryDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GetAuctionsByTypeResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<AuctionSummaryDTO> auctions;
    private String message;

    public GetAuctionsByTypeResponse(List<AuctionSummaryDTO> auctions, String message) {
        this.auctions = auctions;
        this.message = message;
    }

    public List<AuctionSummaryDTO> getAuctions() {
        return auctions;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "GetAuctionsByTypeResponse{" +
                "auctions=" + auctions +
                ", message='" + message + '\'' +
                '}';
    }
}
