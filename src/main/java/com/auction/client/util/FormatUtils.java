package com.auction.client.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class FormatUtils {

    private static final Locale VIETNAM = Locale.of("vi", "VN");

    private FormatUtils() {
    }

    public static String currency(double amount) {
        return NumberFormat.getCurrencyInstance(VIETNAM).format(amount);
    }
}
