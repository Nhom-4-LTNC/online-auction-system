package com.auction.shared.dto;

import com.auction.shared.enums.AuctionStatus;

import java.io.Serial;
import java.io.Serializable;

public class AuctionDetailDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int sellerId;
    private final String sellerUsername;
    private final ItemDTO item;
    private final double startingPrice;
    private final double currentPrice;
    private final double bidStep;
    private final long startTimeMillis; // Đổi tên cho nhất quán
    private final long endTimeMillis;   // Sửa kiểu double -> long
    private final AuctionStatus status;
    private final Integer lastBidderId; // Sửa lỗi viết hoa: id -> Id
    private final String lastBidderUsername;

    public AuctionDetailDTO(int auctionId, int sellerId, String sellerUsername,
                            ItemDTO item, double startingPrice,
                            double currentPrice, double bidStep,
                            long startTimeMillis, long endTimeMillis,
                            AuctionStatus status,
                            Integer lastBidderId, String lastBidderUsername) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.bidStep = bidStep;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.status = status;
        this.lastBidderId = lastBidderId;
        this.lastBidderUsername = lastBidderUsername;
    }

    // Các getters (đã chuẩn hóa tên)
    public int getAuctionId() { return auctionId; }
    public int getSellerId() { return sellerId; }
    public String getSellerUsername() { return sellerUsername; }
    public ItemDTO getItem() { return item; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidStep() { return bidStep; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public AuctionStatus getStatus() { return status; }
    public Integer getLastBidderId() { return lastBidderId; }
    public String getLastBidderUsername() { return lastBidderUsername; }

    @Override
    public String toString() {
        return String.format("AuctionDetailDTO{id=%d, sellerId=%d, sellerUsername='%s', item=%s, startingPrice=%.2f, currentPrice=%.2f, bidStep=%.2f, startTime=%s, endTime=%s, status=%s, lastBidderId=%d, lastBidderUsername='%s'}",
                auctionId, sellerId, sellerUsername, item, startingPrice, currentPrice, bidStep, startTimeMillis, endTimeMillis, status, lastBidderId, lastBidderUsername);
    }
}