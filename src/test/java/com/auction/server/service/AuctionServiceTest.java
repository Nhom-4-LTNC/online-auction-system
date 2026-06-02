package com.auction.server.service;

import com.auction.server.model.auction.Auction;
import com.auction.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NOTE:
 * AuctionService hiện phụ thuộc DB thông qua singleton repos + DatabaseConnection.
 * File này viết các test “smoke/integration-light” phần logic không cần fixture phức tạp.
 *
 * Các test sâu hơn (refreshAndPersistIfChanged/closeAuction) cần dữ liệu fixture MySQL
 * vì service sẽ truy vấn/commit transaction thật.
 */
public class AuctionServiceTest {

    @Test
    void getAuctionModelById_whenAuctionNotFound_shouldThrowResourceNotFoundException() {
        AuctionService service = AuctionService.getInstance();

        // Chọn auctionId “rất khó tồn tại” để tránh flake do dữ liệu DB thay đổi.
        int nonExistingAuctionId = 1_000_000_000;

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAuctionModelById(nonExistingAuctionId),
                "Expected ResourceNotFoundException when auction does not exist"
        );
    }

    @Test
    void auctionModelReturned_shouldHaveValidId() throws Exception {
        AuctionService service = AuctionService.getInstance();

        // Fixture cụ thể (BTL) - lấy auctionId có sẵn trực tiếp từ DB để tránh hardcode.
        // Bước:
        // 1) Lấy danh sách auction (summaries) từ DB
        // 2) Chọn 1 auctionId đầu tiên và test getAuctionModelById
        var summaries = service.getAllAuctionSummaries();
        assertNotNull(summaries, "Auction summaries must not be null");
        assertFalse(summaries.isEmpty(), "DB must have at least 1 auction fixture for this test");

        int someAuctionId = summaries.get(0).getAuctionId();
        assertTrue(someAuctionId > 0, "Selected auctionId must be > 0");


        Auction auction = service.getAuctionModelById(someAuctionId);


        assertNotNull(auction, "Auction should not be null");
        assertTrue(auction.getId() > 0, "Auction id should be > 0");
        assertNotNull(auction.getStatus(), "Auction status should not be null");
        assertNotNull(auction.getItem(), "Auction item should not be null");
        assertNotNull(auction.getItem().getOwner(), "Auction item owner should not be null");

        // Kiểm tra logic thời gian cơ bản
        assertTrue(
                auction.getEndTime() > auction.getStartTime(),
                "Auction endTime must be greater than startTime"
        );

        // Kiểm tra giá cơ bản
        assertTrue(auction.getStartPrice() >= 0, "Auction startPrice must be >= 0");
        assertTrue(auction.getCurrentPrice() >= 0, "Auction currentPrice must be >= 0");

        // bidStep phải > 0 theo validator
        assertTrue(auction.getBidStep() > 0, "Auction bidStep must be > 0");
    }
}

