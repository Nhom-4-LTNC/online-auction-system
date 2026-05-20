package com.auction.exception;

import java.io.Serial;

public class AuctionClosedException extends AuctionAppException {
    public AuctionClosedException(int auctionId) {
        super("Phiên đấu giá (ID: " + auctionId + ") đã đóng, không thể thao tác!");
    }
}
