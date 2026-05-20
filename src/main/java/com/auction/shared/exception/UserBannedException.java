package com.auction.shared.exception;

public class UserBannedException extends AuctionAppException {
    public UserBannedException(long banEndTime) {
        super("Tài khoản của bạn đã bị khóa đến thời điểm: " + banEndTime);
    }
}
