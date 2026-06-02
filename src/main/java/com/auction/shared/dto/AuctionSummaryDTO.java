package com.auction.shared.dto;

import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AuctionSummaryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int itemId;
    private final String itemName;
    private final ItemType itemType;
    private final double currentPrice;
    private final long startTimeMillis;
    private final long endTimeMillis;
    private final AuctionStatus status;
    private final Integer winnerId;
    private final String winnerUsername;

    public AuctionSummaryDTO(int auctionId,
                             int itemId,
                             String itemName,
                             ItemType itemType,
                             double currentPrice,
                             long endTimeMillis,
                             AuctionStatus status) {
        this(auctionId, itemId, itemName, itemType, currentPrice, endTimeMillis, status, null);
    }

    public AuctionSummaryDTO(int auctionId,
                             int itemId,
                             String itemName,
                             ItemType itemType,
                             double currentPrice,
                             long endTimeMillis,
                             AuctionStatus status,
                             Integer winnerId) {
        this(auctionId, itemId, itemName, itemType, currentPrice, 0L, endTimeMillis, status, winnerId, null);
    }

    public AuctionSummaryDTO(int auctionId,
                             int itemId,
                             String itemName,
                             ItemType itemType,
                             double currentPrice,
                             long startTimeMillis,
                             long endTimeMillis,
                             AuctionStatus status,
                             Integer winnerId,
                             String winnerUsername) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.currentPrice = currentPrice;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.status = status;
        this.winnerId = winnerId;
        this.winnerUsername = winnerUsername;
    }

    public int getAuctionId() {
        return auctionId;
    }
    public int getItemId() {return itemId;}
    public String getItemName() {
        return itemName;
    }
    public ItemType getItemType() {
        return itemType;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public long getEndTimeMillis() {
        return endTimeMillis;
    }
    public long getStartTimeMillis() {
        return startTimeMillis;
    }
    public LocalDateTime getStartTime() {
        return toLocalDateTime(startTimeMillis);
    }
    public LocalDateTime getEndTime() {
        return toLocalDateTime(endTimeMillis);
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public Integer getWinnerId() {
        return winnerId;
    }
    public String getWinnerUsername() {
        return winnerUsername;
    }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        if (epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    @Override
    public String toString() {
        return String.format("AuctionSummaryDTO{id=%d, itemId=%d, itemName='%s', itemType='%s', currentPrice=%.2f, startTimeMillis=%d, endTimeMillis=%d, status=%s, winnerId=%s, winnerUsername='%s'}",
                auctionId, itemId, itemName, itemType, currentPrice, startTimeMillis, endTimeMillis, status, winnerId, winnerUsername);
    }

}
