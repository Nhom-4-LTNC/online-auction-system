package com.auction.server.service.policy;

/**
 * Pure anti-sniping rule used after a valid bid is accepted.
 *
 * <p>If a bid lands with 0..2 minutes remaining, the auction end time becomes
 * {@code now + 2 minutes}. The rule is not cumulative against the old end time;
 * it guarantees a minimum response window from the accepted bid time.</p>
 */
public class AntiSnipingPolicy {
    public static final long DEFAULT_WINDOW_MILLIS = 2 * 60 * 1000L;
    public static final long DEFAULT_EXTENSION_MILLIS = 2 * 60 * 1000L;

    private final long windowMillis;
    private final long extensionMillis;

    public AntiSnipingPolicy() {
        this(DEFAULT_WINDOW_MILLIS, DEFAULT_EXTENSION_MILLIS);
    }

    public AntiSnipingPolicy(long windowMillis, long extensionMillis) {
        if (windowMillis < 0 || extensionMillis <= 0) {
            throw new IllegalArgumentException("Invalid anti-sniping policy duration.");
        }
        this.windowMillis = windowMillis;
        this.extensionMillis = extensionMillis;
    }

    /**
     * Returns true when the auction is still active and inside the anti-sniping window.
     */
    public boolean shouldExtend(long nowMillis, long endTimeMillis) {
        long remainingMillis = endTimeMillis - nowMillis;
        return remainingMillis >= 0 && remainingMillis <= windowMillis;
    }

    /**
     * Calculates the replacement end time after a qualifying bid.
     */
    public long calculateNewEndTime(long nowMillis) {
        return nowMillis + extensionMillis;
    }
}
