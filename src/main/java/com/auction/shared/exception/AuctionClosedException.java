package com.auction.shared.exception;

public class AuctionClosedException extends AuctionAppException {
    public AuctionClosedException(int auctionId) {
        super("Phiên đấu giá (ID: " + auctionId + ") đã đóng, không thể thao tác!");
    }
}
