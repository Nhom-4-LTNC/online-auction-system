package com.auction.client.service;

import com.auction.client.network.Client;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.bid.SendAuctionChatRequest;
import com.auction.shared.protocol.bid.SendAuctionChatResponse;

public class AuctionChatClientService extends BaseClientService {

    private final Client client = Client.getInstance();

    public AuctionChatClientService() {
        super();
    }

    public SendAuctionChatResponse sendChat(int auctionId, String message) throws Exception {
        SendAuctionChatRequest payload = new SendAuctionChatRequest(auctionId, message);
        Request<SendAuctionChatRequest> request = new Request<>(ActionType.SEND_AUCTION_CHAT, payload);

        Response<?> response = client.sendRequestAndWait(request, 5000);
        if (response == null) {
            return null;
        }
        if (response.getPayload() instanceof SendAuctionChatResponse res) {
            return res;
        }
        return null;
    }
}

