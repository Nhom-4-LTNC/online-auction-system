package com.auction.server.model.auction;

import com.auction.server.model.Bid;

public interface AuctionObserver {
    void onNewBidPlace(Bid transaction);
    void onAuctionClosed(Auction auction);
}
