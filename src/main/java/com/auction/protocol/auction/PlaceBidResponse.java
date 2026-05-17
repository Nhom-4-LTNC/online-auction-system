package com.auction.protocol.auction;

import com.auction.protocol.ActionType;

public class PlaceBidResponse {
    public ActionType actionType;
    public int auctionId;
    public int userId;
    public double amount;
    public String message;

    public PlaceBidResponse(ActionType actionType, int auctionId, int userId, double amount, String message) {
        this.actionType = actionType;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.message = message;
    }
    public PlaceBidResponse() {}

    public ActionType getResponseType() { return actionType; }
    public int getAuctionId() { return auctionId; }
    public int getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getMessage() { return message; }
}
