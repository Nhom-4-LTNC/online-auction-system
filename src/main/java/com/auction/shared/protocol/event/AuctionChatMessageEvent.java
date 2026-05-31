package com.auction.shared.protocol.event;

import java.io.Serial;
import java.io.Serializable;

public class AuctionChatMessageEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int senderId;
    private final String senderUsername;
    private final String message;
    private final long timestamp;
    private final boolean system;

    public AuctionChatMessageEvent(
            int auctionId,
            int senderId,
            String senderUsername,
            String message,
            long timestamp,
            boolean system
    ) {
        this.auctionId = auctionId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timestamp = timestamp;
        this.system = system;
    }

    public int getAuctionId() { return auctionId; }
    public int getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isSystem() { return system; }

    @Override
    public String toString() {
        return "AuctionChatMessageEvent{" +
                "auctionId=" + auctionId +
                ", senderId=" + senderId +
                ", senderUsername='" + senderUsername + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", system=" + system +
                '}';
    }
}

