package com.auction.server.model.auction;

import com.auction.server.model.Bid;
import com.auction.server.model.user.User;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionDomainTest {

    @Test
    void placeBid_validBid_shouldUpdateCurrentPriceAndLeader() throws Exception {
        User seller = TestDataFactory.user(1, "seller");
        User bidder = TestDataFactory.user(2, "bidder");
        Auction auction = TestDataFactory.runningAuction(seller);

        Bid bid = auction.placeBid(bidder, 120_000);

        assertEquals(120_000, auction.getCurrentPrice(), 0.000001);
        assertSame(bidder, auction.getLastBidder());
        assertEquals(bidder.getId(), bid.getBidderId());
        assertEquals(120_000, bid.getAmount(), 0.000001);
    }

    @Test
    void placeBid_lowBid_shouldThrowException() throws Exception {
        User seller = TestDataFactory.user(1, "seller");
        User leadingBidder = TestDataFactory.user(2, "leader");
        User bidder = TestDataFactory.user(3, "bidder");
        Auction auction = TestDataFactory.runningAuction(seller);
        auction.setCurrentPrice(100_000);
        auction.setLastBidder(leadingBidder);

        assertThrows(InvalidBidException.class, () -> auction.placeBid(bidder, 105_000));
    }

    @Test
    void placeBid_sellerSelfBid_shouldThrowException() {
        User seller = TestDataFactory.user(1, "seller");
        Auction auction = TestDataFactory.runningAuction(seller);

        assertThrows(InvalidBidException.class, () -> auction.placeBid(seller, 120_000));
    }

    @Test
    void placeBid_finishedAuction_shouldThrowException() {
        assertClosedAuctionRejectsBid(AuctionStatus.FINISHED);
    }

    @Test
    void placeBid_paidAuction_shouldThrowException() {
        assertClosedAuctionRejectsBid(AuctionStatus.PAID);
    }

    @Test
    void placeBid_canceledAuction_shouldThrowException() {
        assertClosedAuctionRejectsBid(AuctionStatus.CANCELED);
    }

    @Test
    void placeBid_openAuction_shouldThrowException() {
        User seller = TestDataFactory.user(1, "seller");
        User bidder = TestDataFactory.user(2, "bidder");
        Auction auction = TestDataFactory.openAuction(seller);

        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bidder, 120_000));
    }

    private void assertClosedAuctionRejectsBid(AuctionStatus status) {
        User seller = TestDataFactory.user(1, "seller");
        User bidder = TestDataFactory.user(2, "bidder");
        Auction auction = TestDataFactory.auctionWithStatus(seller, status);

        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bidder, 120_000));
    }
}
