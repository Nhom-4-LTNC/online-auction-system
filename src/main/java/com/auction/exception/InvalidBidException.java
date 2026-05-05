package com.auction.exception;

import java.io.Serial;

public class InvalidBidException extends Exception {
    @Serial
    private static final long serialVersionUID = 4703852084208369501L;

    public InvalidBidException(String message) {
        super(message);
    }
}
