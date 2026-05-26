package com.auction.shared.protocol;

import java.io.Serializable;

public enum AuctionUpdateType implements Serializable {
    BID_PLACED,
    AUCTION_CLOSED,
    PAYMENT_COMPLETED,
    AUCTION_CANCELLED,
    AUCTION_CREATED
}
