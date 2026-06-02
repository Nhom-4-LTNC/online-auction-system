package com.auction.shared.dto;

import com.auction.shared.enums.AuctionStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
    private final long startTimeMillis;
    private final long endTimeMillis;
    private final AuctionStatus status;
    private final Integer lastBidderId;
    private final String lastBidderUsername;
    private final Integer winnerId;
    private final String winnerUsername;

    public AuctionDetailDTO(int auctionId, int sellerId, String sellerUsername,
                            ItemDTO item, double startingPrice,
                            double currentPrice, double bidStep,
                            long startTimeMillis, long endTimeMillis,
                            AuctionStatus status,
                            Integer lastBidderId, String lastBidderUsername) {
        this(auctionId, sellerId, sellerUsername, item, startingPrice, currentPrice, bidStep,
                startTimeMillis, endTimeMillis, status, lastBidderId, lastBidderUsername, null, null);
    }

    public AuctionDetailDTO(int auctionId, int sellerId, String sellerUsername,
                            ItemDTO item, double startingPrice,
                            double currentPrice, double bidStep,
                            long startTimeMillis, long endTimeMillis,
                            AuctionStatus status,
                            Integer lastBidderId, String lastBidderUsername,
                            Integer winnerId, String winnerUsername) {
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
        this.winnerId = winnerId;
        this.winnerUsername = winnerUsername;
    }

    public int getAuctionId() { return auctionId; }
    public int getSellerId() { return sellerId; }
    public String getSellerUsername() { return sellerUsername; }
    public ItemDTO getItem() { return item; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidStep() { return bidStep; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public LocalDateTime getStartTime() { return toLocalDateTime(startTimeMillis); }
    public LocalDateTime getEndTime() { return toLocalDateTime(endTimeMillis); }
    public AuctionStatus getStatus() { return status; }
    public Integer getLastBidderId() { return lastBidderId; }
    public String getLastBidderUsername() { return lastBidderUsername; }
    public Integer getWinnerId() { return winnerId; }
    public String getWinnerUsername() { return winnerUsername; }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        if (epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    @Override
    public String toString() {
        return String.format("AuctionDetailDTO{id=%d, sellerId=%d, sellerUsername='%s', item=%s, startingPrice=%.2f, currentPrice=%.2f, bidStep=%.2f, startTime=%d, endTime=%d, status=%s, lastBidderId=%s, lastBidderUsername='%s', winnerId=%s, winnerUsername='%s'}",
                auctionId, sellerId, sellerUsername, item, startingPrice, currentPrice, bidStep,
                startTimeMillis, endTimeMillis, status, lastBidderId, lastBidderUsername, winnerId, winnerUsername);
    }
}
