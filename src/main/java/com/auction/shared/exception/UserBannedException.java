package com.auction.shared.exception;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UserBannedException extends AuctionAppException {
    private static final DateTimeFormatter BAN_END_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                    .withZone(ZoneId.systemDefault());

    public UserBannedException(long banEndTimeMillis) {
        super("Tài khoản của bạn đã bị khóa đến thời điểm: " + formatBanEndTime(banEndTimeMillis));
    }

    private static String formatBanEndTime(long banEndTimeMillis) {
        if (banEndTimeMillis <= 0) {
            return "không xác định";
        }
        return BAN_END_TIME_FORMATTER.format(Instant.ofEpochMilli(banEndTimeMillis));
    }
}

