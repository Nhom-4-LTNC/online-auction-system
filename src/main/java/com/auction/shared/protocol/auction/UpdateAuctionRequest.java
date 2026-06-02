package com.auction.shared.protocol.auction;

import com.auction.shared.dto.ItemDTO;

import java.io.Serial;
import java.io.Serializable;

public class UpdateAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final ItemDTO itemDto;
    private final long startTime;
    private final long endTime;

    public UpdateAuctionRequest(int auctionId, ItemDTO itemDto, long startTime, long endTime) {
        this.auctionId = auctionId;
        this.itemDto = itemDto;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public ItemDTO getItemDto() {
        return itemDto;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return String.format("UpdateAuctionRequest{auctionId=%d, itemDto=%s, startTime=%d, endTime=%d}",
                auctionId, itemDto, startTime, endTime);
    }
}
