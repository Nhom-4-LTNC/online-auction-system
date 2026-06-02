package com.auction.shared.protocol.event;

import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.protocol.AuctionUpdateType;

import java.io.Serial;
import java.io.Serializable;

public class AuctionUpdatedEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final AuctionUpdateType updateType;
    private final AuctionSummaryDTO summary;
    private final BidDTO lastestBid;
    private final String message;
    private final long timestamp;
    public AuctionUpdatedEvent(int auctionId, AuctionUpdateType updateType, AuctionSummaryDTO summary, BidDTO lastestBid, String message, long timestamp) {
        this.auctionId = auctionId;
        this.updateType = updateType;
        this.summary = summary;
        this.lastestBid = lastestBid;
        this.message = message;
        this.timestamp = timestamp;
    }
    public int getAuctionId() { return auctionId; }
    public AuctionUpdateType getUpdateType() { return updateType; }
    public AuctionSummaryDTO getSummary() { return summary; }
    public BidDTO getLastestBid() { return lastestBid; }
    public BidDTO getLatestBid() { return lastestBid; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    @Override
    public String toString() {
        return String.format("AuctionUpdatedEvent{auctionId=%d, updateType=%s, summary=%s, lastestBid=%s, message='%s', timestamp=%d}",
                auctionId, updateType, summary, lastestBid, message, timestamp);
    }

}
