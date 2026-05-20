package com.auction.shared.dto;

import java.io.Serial;
import java.io.Serializable;

public class BidDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int bidId;
    private final int auctionId;
    private final int bidderId;
    private final String bidderUsername;
    private final double amount;
    private final long bidTime;

    public BidDTO(int bidId, int auctionId,
                  int bidderId,
                  String bidderUsername,
                  double amount, long bidTime) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.bidTime = bidTime;

    }
    public int getBidId() { return bidId; }
    public int getAuctionId() { return auctionId; }
    public int getBidderId() { return bidderId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public long getBidTime() { return bidTime; }

}
