package com.auction.testutil;

import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Art;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.Role;

public final class TestDataFactory {
    private static final long DEFAULT_START_PRICE = 100_000L;
    private static final long DEFAULT_BID_STEP = 10_000L;

    private TestDataFactory() {
    }

    public static User user(int id, String username) {
        User user = new User(id, username, "password", username + "@example.test", Role.USER);
        user.setBalance(1_000_000);
        return user;
    }

    public static Item itemOwnedBy(User owner) {
        return new Art(1, "Test artwork", "Domain test item", owner, DEFAULT_START_PRICE, "Tester", 2024);
    }

    public static Auction runningAuction(User seller) {
        long now = System.currentTimeMillis();
        Auction auction = new Auction(1, itemOwnedBy(seller), DEFAULT_BID_STEP, now - 60_000L, now + 60_000L);
        auction.setStatus(AuctionStatus.RUNNING);
        return auction;
    }

    public static Auction openAuction(User seller) {
        long now = System.currentTimeMillis();
        Auction auction = new Auction(1, itemOwnedBy(seller), DEFAULT_BID_STEP, now + 60_000L, now + 120_000L);
        auction.setStatus(AuctionStatus.OPEN);
        return auction;
    }

    public static Auction auctionWithStatus(User seller, AuctionStatus status) {
        Auction auction = runningAuction(seller);
        auction.setStatus(status);
        return auction;
    }
}
