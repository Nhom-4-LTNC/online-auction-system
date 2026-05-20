package com.auction.shared.protocol.bid;
import com.auction.shared.dto.AuctionDetailDTO;

import java.io.Serial;
import java.io.Serializable;

public class PlaceBidResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AuctionDetailDTO updatedAuction;
    private final String message;
    public PlaceBidResponse(AuctionDetailDTO auction, String message) {
        this.updatedAuction = auction;
        this.message = message;
    }

    public AuctionDetailDTO getAuction() { return updatedAuction; }
    public String getMessage() { return message; }
}
