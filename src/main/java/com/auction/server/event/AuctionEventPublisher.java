package com.auction.server.event;

import com.auction.server.Server;
import com.auction.server.handler.ClientHandler;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;

/**
 * Publishes server-side auction changes as socket push events.
 *
 * <p>The server wraps {@link AuctionUpdatedEvent} in a normal
 * {@link Response} with action {@link ActionType#AUCTION_UPDATED} and
 * broadcasts it to logged-in clients. Clients filter by auctionId; no client
 * polling or server model transfer is required.</p>
 */
public class AuctionEventPublisher {

    private static final AuctionEventPublisher instance = new AuctionEventPublisher();

    private AuctionEventPublisher() {}

    public static AuctionEventPublisher getInstance() {
        return instance;
    }

    /**
     * Broadcasts an auction update to every currently logged-in client.
     */
    public void publishAuctionUpdated(AuctionUpdatedEvent event) {
        if (event == null) return;

        Response<AuctionUpdatedEvent> response = Response.success(
                ActionType.AUCTION_UPDATED,
                event
        );
        System.out.println("[AuctionEventPublisher] Publish AUCTION_UPDATED auctionId="
                + event.getAuctionId() + ", type=" + event.getUpdateType());
        Server.broadcastToLoggedIn(response);
    }

    /**
     * Broadcasts an auction update to logged-in clients except the requester.
     */
    public void publishAuctionUpdatedExcept(AuctionUpdatedEvent event, ClientHandler excludedClient) {
        if (event == null) return;

        Response<AuctionUpdatedEvent> response = Response.success(
                ActionType.AUCTION_UPDATED,
                event
        );
        System.out.println("[AuctionEventPublisher] Publish AUCTION_UPDATED auctionId="
                + event.getAuctionId() + ", type=" + event.getUpdateType()
                + ", excluding requester");
        Server.broadcastToLoggedInExcept(response, excludedClient);
    }

    public void publishAuctionUpdated(
            int auctionId,
            AuctionUpdateType updateType,
            AuctionSummaryDTO summary,
            BidDTO latestBid,
            String message
    ) {
        publishAuctionUpdated(new AuctionUpdatedEvent(
                auctionId,
                updateType,
                summary,
                latestBid,
                message,
                System.currentTimeMillis()
        ));
    }

    public void publishAuctionUpdatedExcept(
            int auctionId,
            AuctionUpdateType updateType,
            AuctionSummaryDTO summary,
            BidDTO latestBid,
            String message,
            ClientHandler excludedClient
    ) {
        publishAuctionUpdatedExcept(new AuctionUpdatedEvent(
                auctionId,
                updateType,
                summary,
                latestBid,
                message,
                System.currentTimeMillis()
        ), excludedClient);
    }
}
