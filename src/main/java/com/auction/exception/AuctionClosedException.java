package com.auction.exception;

import java.io.Serial;

public class AuctionClosedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4475551439637346986L;

    public AuctionClosedException(String message) {
        super(message);
    }
}
