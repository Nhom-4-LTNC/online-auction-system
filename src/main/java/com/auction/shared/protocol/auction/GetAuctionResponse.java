package com.auction.shared.protocol.auction;

import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.BidDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GetAuctionResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AuctionDetailDTO auction;
    private final List<BidDTO> recentBids;
    private String message;

    public GetAuctionResponse(AuctionDetailDTO auction,
                              List<BidDTO> recentBids, String message) {
        this.auction = auction;
        this.recentBids = recentBids;
        this.message = message;
    }

    public AuctionDetailDTO getAuction() { return auction; }
    public List<BidDTO> getRecentBids() { return recentBids; }
    public String getMessage() { return message; }
}
