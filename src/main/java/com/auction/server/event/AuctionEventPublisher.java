package com.auction.server.event;

import com.auction.server.Server;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;

public class AuctionEventPublisher {

    private static final AuctionEventPublisher instance = new AuctionEventPublisher();

    private AuctionEventPublisher() {}

    public static AuctionEventPublisher getInstance() {
        return instance;
    }

    public void publishAuctionUpdated(AuctionUpdatedEvent event) {
        if (event == null) return;

        Response<AuctionUpdatedEvent> response = Response.success(
                ActionType.AUCTION_UPDATED,
                event
        );
        System.out.println("[AuctionEventPublisher] Publish AUCTION_UPDATED: " + event);
        Server.broadcastToLoggedIn(response);
    }
}
