package com.auction.model.auction;

import java.io.Serial;
import java.io.Serializable;

import com.auction.model.item.ItemType;

public class AuctionListItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int itemId;
    private final String itemName;
    private final ItemType category;
    private final double currentPrice;
    private final AuctionStatus status;
    private final long endTime;

    public AuctionListItem(int auctionId,
                             int itemId,
                             String itemName,
                             ItemType category,
                             double currentPrice,
                             AuctionStatus status,
                             long endTime) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.category = category;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemType getCategory() {
        return category;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return String.format("Auction #%d | %s (%s) | %.2f | %s",
                auctionId,
                itemName,
                category,
                currentPrice,
                status);
    }
}

