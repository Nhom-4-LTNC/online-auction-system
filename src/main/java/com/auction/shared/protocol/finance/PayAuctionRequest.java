package com.auction.shared.protocol.finance;

import java.io.Serial;
import java.io.Serializable;

public class PayAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;

    public PayAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}
