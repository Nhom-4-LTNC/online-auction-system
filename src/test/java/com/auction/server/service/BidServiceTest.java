package com.auction.server.service;

import com.auction.server.model.Bid;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InsufficientFundsException;
import com.auction.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BidServiceTest - test smoke/integration-light.
 *
 * Mục tiêu của BidService:
 *  - getBidsByAuctionId: trả về bids có auctionId khớp
 *  - placeBid:
 *      + auction không tồn tại -> ResourceNotFoundException
 *      + auction FINISHED -> AuctionClosedException (hoặc không biddable theo implement)
 *      + không đủ tiền -> InsufficientFundsException
 */
public class BidServiceTest {

    private int getFirstAuctionId(AuctionService auctionService) throws Exception {
        List<?> summaries = auctionService.getAllAuctionSummaries();
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty(), "DB phải có ít nhất 1 auction fixture để chạy test");

        Object first = summaries.get(0);
        return (Integer) first.getClass().getMethod("getAuctionId").invoke(first);
    }

    private int findFinishedAuctionId(AuctionService auctionService) throws Exception {
        for (Object s : auctionService.getAllAuctionSummaries()) {
            Object statusObj = s.getClass().getMethod("getStatus").invoke(s);
            if (statusObj != null && statusObj.toString().equals("FINISHED")) {
                return (Integer) s.getClass().getMethod("getAuctionId").invoke(s);
            }
        }
        return -1;
    }

    private int findBiddableAuctionId(AuctionService auctionService) throws Exception {
        // Dựa theo AuctionStatus enum trong project: OPEN hoặc RUNNING thường là biddable.
        for (Object s : auctionService.getAllAuctionSummaries()) {
            Object statusObj = s.getClass().getMethod("getStatus").invoke(s);
            if (statusObj != null) {
                String st = statusObj.toString();
                if (st.equals("OPEN") || st.equals("RUNNING")) {
                    return (Integer) s.getClass().getMethod("getAuctionId").invoke(s);
                }
            }
        }
        return -1;
    }

    @Test
    void getBidsByAuctionId_whenAuctionHasData_shouldReturnBidsWithMatchingAuctionId() throws Exception {
        BidService bidService = BidService.getInstance();
        AuctionService auctionService = AuctionService.getInstance();

        int auctionId = getFirstAuctionId(auctionService);

        List<Bid> bids = bidService.getBidsByAuctionId(auctionId);
        assertNotNull(bids);

        for (Bid bid : bids) {
            assertEquals(auctionId, bid.getAuctionId(), "Bid.auctionId phải khớp auctionId");
            assertTrue(bid.getAmount() >= 0);
        }
    }

    @Test
    void placeBid_whenAuctionDoesNotExist_shouldThrowResourceNotFoundException() {
        BidService bidService = BidService.getInstance();

        int bidderId = 30001; // theo fixture users trong schema.sql
        int nonExistingAuctionId = 1_000_000_000;

        assertThrows(
                ResourceNotFoundException.class,
                () -> bidService.placeBid(bidderId, nonExistingAuctionId, 100.0)
        );
    }

    @Test
    void placeBid_whenAuctionIsAlreadyFinished_shouldThrowAuctionClosedException() throws Exception {
        BidService bidService = BidService.getInstance();
        AuctionService auctionService = AuctionService.getInstance();

        int finishedAuctionId = findFinishedAuctionId(auctionService);
        assumeTrue(finishedAuctionId > 0, "DB hiện không có auction FINISHED để test");

        int bidderId = 30001;
        double amount = 1.0;

        assertThrows(
                AuctionClosedException.class,
                () -> bidService.placeBid(bidderId, finishedAuctionId, amount)
        );
    }

    @Test
    void placeBid_whenInsufficientFunds_shouldThrowInsufficientFundsException() throws Exception {
        BidService bidService = BidService.getInstance();
        AuctionService auctionService = AuctionService.getInstance();
        WalletService walletService = WalletService.getInstance();

        int auctionId = findBiddableAuctionId(auctionService);
        assumeTrue(auctionId > 0, "DB hiện không có auction OPEN/RUNNING để test InsufficientFunds");

        int bidderId = 30001;

        // Chọn amount > availableForThisBid để chắc chắn ném InsufficientFundsException.
        double available = walletService.getAvailableBalanceForBid(
                com.auction.server.database.DatabaseConnection.getConnection(),
                bidderId,
                auctionId
        );

        double amount = available + 1.0;
        if (amount <= 0) amount = 1.0;

        final double finalAmount = amount;
        assertThrows(
                InsufficientFundsException.class,
                () -> bidService.placeBid(bidderId, auctionId, finalAmount)
        );
    }
}

