package com.auction.shared.exception;

/**
 * Ngoại lệ được ném ra khi trùng email, username, sản phẩm,...
 */
public class DuplicateResourceException extends AuctionAppException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
