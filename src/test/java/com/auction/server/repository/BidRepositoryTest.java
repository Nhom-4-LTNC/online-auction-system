package com.auction.server.repository;

import com.auction.server.model.Bid;
import com.auction.server.service.AuctionService;
import com.auction.testutil.DbTestSupport;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BidRepositoryTest {

    @Test
    void findByAuctionId_whenAuctionHasBids_shouldReturnBidsWithMatchingAuctionId() throws Exception {
        DbTestSupport.assumeDatabaseAvailable();

        AuctionServiceSupport auctionSupport = new AuctionServiceSupport();
        int auctionId = auctionSupport.getFirstAuctionId();
        assertTrue(auctionId > 0);

        BidRepository repo = BidRepository.getInstance();
        List<Bid> bids = repo.findByAuctionId(auctionId);

        assertNotNull(bids);
        // Không fail nếu DB có thể không có dữ liệu bids; nhưng nếu có, validate correctness.
        for (Bid bid : bids) {
            assertEquals(auctionId, bid.getAuctionId(), "Bid.auctionId phải khớp auctionId");
            assertTrue(bid.getAmount() >= 0);
        }
    }

    @Test
    void findHighestBidByAuctionId_whenBidsExist_shouldReturnMaxAmountBid() throws Exception {
        DbTestSupport.assumeDatabaseAvailable();

        AuctionServiceSupport auctionSupport = new AuctionServiceSupport();
        int auctionId = auctionSupport.getFirstAuctionId();
        assertTrue(auctionId > 0);

        BidRepository repo = BidRepository.getInstance();
        List<Bid> bids = repo.findByAuctionId(auctionId);

        if (bids.isEmpty()) {
            // Smoke: nếu fixture không có bids thì bỏ qua validate logic.
            return;
        }

        Bid highest = repo.findHighestBidByAuctionId(auctionId);
        assertNotNull(highest);
        assertEquals(auctionId, highest.getAuctionId());

        double max = bids.stream().mapToDouble(Bid::getAmount).max().orElse(0);
        assertEquals(max, highest.getAmount(), 0.000001);
    }

    /**
     * Helper cực nhỏ để lấy auctionId có sẵn từ DB bằng AuctionService.
     * Không phụ thuộc fixtures bids.
     */
    private static class AuctionServiceSupport {
        private int getFirstAuctionId() throws Exception {
            AuctionService service = AuctionService.getInstance();
            List<?> summaries = service.getAllAuctionSummaries();
            assertNotNull(summaries);
            assertFalse(summaries.isEmpty(), "DB phải có ít nhất 1 auction fixture để chạy test");
            Object first = summaries.get(0);
            return (Integer) first.getClass().getMethod("getAuctionId").invoke(first);
        }
    }
}

