package com.auction.exception;

import java.io.Serial;

public class InsufficientFundsException extends AuctionAppException {
    public InsufficientFundsException() {
        super("Số dư tài khoản không đủ để thực hiện giao dịch này!");
    }
}
