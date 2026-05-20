package com.auction.shared.exception;

public class ResourceNotFoundException extends AuctionAppException {
    public ResourceNotFoundException(String resourceName, int id) {
        super("Không tìm thấy " + resourceName + " với ID: " + id);

    }
}
