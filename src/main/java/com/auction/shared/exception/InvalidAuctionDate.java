package com.auction.shared.exception;

public class InvalidAuctionDate extends IllegalArgumentException {
    public InvalidAuctionDate(String message) {
        super(message);
    }
}
