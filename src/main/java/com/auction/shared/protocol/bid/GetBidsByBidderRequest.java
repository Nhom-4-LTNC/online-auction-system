package com.auction.shared.protocol.bid;

import java.io.Serial;

public class GetBidsByBidderRequest implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private final int bidderId;

    public GetBidsByBidderRequest(int bidderId) {
        this.bidderId = bidderId;
    }

    public int getBidderId() {
        return bidderId;
    }

    @Override
    public String toString() {
        return String.format("GetBidsByBidderRequest{bidderId=%d}", bidderId);
    }
}
