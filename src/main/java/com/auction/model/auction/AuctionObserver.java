package com.auction.model.auction;

import com.auction.model.BidTransaction;

public interface AuctionObserver {
    void onNewBidPlace(BidTransaction transaction);
    void onAuctionClosed(Auction auction);
}
