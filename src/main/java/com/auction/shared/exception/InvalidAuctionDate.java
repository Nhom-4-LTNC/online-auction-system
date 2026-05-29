package com.auction.shared.exception;

public class InvalidAuctionDate extends RuntimeException {
    public InvalidAuctionDate(String message) {
        super(message);
    }
}
