package com.auction.shared.dto;

import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;

import java.io.Serial;
import java.io.Serializable;

public class AuctionSummaryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int itemId;
    private final String itemName;
    private final ItemType itemType;
    private final double currentPrice;
    private final long endTimeMillis;
    private final AuctionStatus status;

    public AuctionSummaryDTO(int auctionId,
                             int itemId,
                             String itemName,
                             ItemType itemType,
                             double currentPrice,
                             long endTimeMillis,
                             AuctionStatus status) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.currentPrice = currentPrice;
        this.endTimeMillis = endTimeMillis;
        this.status = status;
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
    public AuctionStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("AuctionSummaryDTO{id=%d, itemId=%d, itemName='%s', itemType='%s', currentPrice=%.2f, endTimeMillis=%d, status=%s}",
                auctionId, itemId, itemName, itemType, currentPrice, endTimeMillis, status);
    }

}