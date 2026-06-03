package com.auction.server.model.auction;

import com.auction.server.model.user.User;
import com.auction.shared.enums.AuctionStatus;
import com.auction.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionStatusTest {

    @Test
    void refreshStatus_beforeStart_shouldRemainOpen() {
        long now = 1_000_000L;
        Auction auction = auction(now + 10_000L, now + 20_000L);

        auction.refreshStatus(now);

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void refreshStatus_betweenStartAndEnd_shouldBecomeRunning() {
        long now = 1_000_000L;
        Auction auction = auction(now - 10_000L, now + 10_000L);

        auction.refreshStatus(now);

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void refreshStatus_afterEnd_shouldBecomeFinished() {
        long now = 1_000_000L;
        Auction auction = auction(now - 20_000L, now - 10_000L);

        auction.refreshStatus(now);

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void refreshStatus_paidAuction_shouldRemainPaid() {
        assertTerminalStatusDoesNotChange(AuctionStatus.PAID);
    }

    @Test
    void refreshStatus_canceledAuction_shouldRemainCanceled() {
        assertTerminalStatusDoesNotChange(AuctionStatus.CANCELED);
    }

    @Test
    void refreshStatus_finishedAuction_shouldRemainFinishedOrNotBecomeRunning() {
        assertTerminalStatusDoesNotChange(AuctionStatus.FINISHED);
    }

    private Auction auction(long startTimeMillis, long endTimeMillis) {
        User seller = TestDataFactory.user(1, "seller");
        Auction auction = new Auction(1, TestDataFactory.itemOwnedBy(seller), 10_000, startTimeMillis, endTimeMillis);
        auction.setStatus(AuctionStatus.OPEN);
        return auction;
    }

    private void assertTerminalStatusDoesNotChange(AuctionStatus terminalStatus) {
        long now = 1_000_000L;
        Auction auction = auction(now - 10_000L, now + 10_000L);
        auction.setStatus(terminalStatus);

        auction.refreshStatus(now);

        assertEquals(terminalStatus, auction.getStatus());
    }
}
