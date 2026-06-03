package com.auction.shared.protocol;

import java.io.Serializable;

/**
 * Business reason for an {@code AUCTION_UPDATED} server push.
 *
 * <p>Clients use this value to decide whether they can apply a lightweight
 * latestBid update or should reload full detail/list data.</p>
 */
public enum AuctionUpdateType implements Serializable {
    BID_PLACED,
    AUCTION_CLOSED,
    PAYMENT_COMPLETED,
    AUCTION_CANCELED,
    AUCTION_CREATED,
    AUCTION_ITEM_UPDATED,
    AUCTION_STARTED,
    AUCTION_FINISHED
}
