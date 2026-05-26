package com.auction.shared.exception;

public class InsufficientFundsException extends AuctionAppException {
    public InsufficientFundsException() {
        super("Số dư tài khoản không đủ để thực hiện giao dịch này!");
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}
