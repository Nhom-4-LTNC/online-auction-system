package com.auction.server.service;

import com.auction.server.model.Bid;
import com.auction.server.model.auction.Auction;

public interface AuctionObserver {
    void onNewBidPlace(Bid transaction);
    void onAuctionClosed(Auction auction);
}
