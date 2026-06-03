package com.auction.shared.protocol.event;

import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.protocol.AuctionUpdateType;

import java.io.Serial;
import java.io.Serializable;

/**
 * Serializable realtime payload for {@code AUCTION_UPDATED}.
 *
 * <p>The payload is server-to-client only. It carries a summary DTO for list
 * and detail refreshes plus an optional latest bid DTO for fast bid-history
 * updates. Clients should still filter by auctionId before applying it.</p>
 */
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
    /**
     * @return latest bid payload. Kept for compatibility with the historical
     * misspelled field name.
     */
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
