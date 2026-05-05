package com.auction.exception;

import java.io.Serial;

public class InsufficientFundsException extends Exception {
    @Serial
    private static final long serialVersionUID = -1900059234814267109L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
