package com.auction.exception;

import java.io.Serial;

public class InvalidBidException extends AuctionAppException {
    public InvalidBidException(String message) {
        super(message);
    }
}
