package com.auction.server.service.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiSnipingPolicyTest {

    private final AntiSnipingPolicy policy = new AntiSnipingPolicy();

    @Test
    void shouldExtend_whenRemainingWithinWindow() {
        long now = 1_000_000L;
        long endTime = now + AntiSnipingPolicy.DEFAULT_WINDOW_MILLIS - 1;

        assertTrue(policy.shouldExtend(now, endTime));
    }

    @Test
    void shouldExtend_whenRemainingExactlyAtWindowBoundary() {
        long now = 1_000_000L;
        long endTime = now + AntiSnipingPolicy.DEFAULT_WINDOW_MILLIS;

        assertTrue(policy.shouldExtend(now, endTime));
    }

    @Test
    void shouldNotExtend_whenRemainingGreaterThanWindow() {
        long now = 1_000_000L;
        long endTime = now + AntiSnipingPolicy.DEFAULT_WINDOW_MILLIS + 1;

        assertFalse(policy.shouldExtend(now, endTime));
    }

    @Test
    void shouldNotExtend_whenAuctionAlreadyEnded() {
        long now = 1_000_000L;
        long endTime = now - 1;

        assertFalse(policy.shouldExtend(now, endTime));
    }

    @Test
    void calculateNewEndTime_shouldReturnNowPlusExtension() {
        long now = 1_000_000L;

        assertEquals(now + AntiSnipingPolicy.DEFAULT_EXTENSION_MILLIS, policy.calculateNewEndTime(now));
    }
}
