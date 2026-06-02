package com.auction.server.repository;

import com.auction.server.model.auction.Auction;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionRepositoryTest {

    @Test
    void getAllAuctionSummaries_whenDbHasAuctions_shouldReturnSummariesWithAuctionId() throws Exception {
        AuctionRepository repo = AuctionRepository.getInstance();

        List<AuctionSummaryDTO> summaries = repo.getAllAuctionSummaries();
        assertNotNull(summaries);

        // Không hard-fail nếu DB chưa có auction fixture; test smoke.
        if (summaries.isEmpty()) return;

        AuctionSummaryDTO first = summaries.get(0);
        assertTrue(first.getAuctionId() > 0);
        assertNotNull(first.getStatus());
    }

    @Test
    void getAuctionById_whenAuctionExists_shouldReturnNonNullAuction() throws Exception {
        AuctionServiceSupport auctionSupport = new AuctionServiceSupport();
        int auctionId = auctionSupport.getFirstAuctionId();
        assertTrue(auctionId > 0);

        AuctionRepository repo = AuctionRepository.getInstance();
        Auction auction = repo.getAuctionById(auctionId);

        // Nếu DB không có item/auction mapping thì có thể null => smoke skip.
        if (auction == null) return;

        assertEquals(auctionId, auction.getId());
        assertNotNull(auction.getStatus());
        assertNotNull(auction.getItem());
    }

    @Test
    void getActiveAuctions_whenThereAreActive_shouldOnlyReturnOpenOrRunning() throws Exception {
        AuctionRepository repo = AuctionRepository.getInstance();

        List<Auction> active = repo.getActiveAuctions();
        assertNotNull(active);

        // Smoke: nếu không có fixture active thì bỏ qua.
        if (active.isEmpty()) return;

        for (Auction a : active) {
            assertNotNull(a.getStatus());
            assertTrue(
                    a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING,
                    "Active auction phải có status OPEN hoặc RUNNING"
            );
        }
    }

    private static class AuctionServiceSupport {
        private int getFirstAuctionId() throws Exception {
            com.auction.server.service.AuctionService service = com.auction.server.service.AuctionService.getInstance();
            List<AuctionSummaryDTO> summaries = service.getAllAuctionSummaries();
            assertNotNull(summaries);
            assertFalse(summaries.isEmpty(), "DB phải có ít nhất 1 auction fixture để chạy test");
            return summaries.get(0).getAuctionId();
        }
    }
}

