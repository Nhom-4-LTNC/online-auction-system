package com.auction.model.auction;

import com.auction.model.Bid;

public interface AuctionObserver {
    void onNewBidPlace(Bid transaction);
    void onAuctionClosed(Auction auction);
}
