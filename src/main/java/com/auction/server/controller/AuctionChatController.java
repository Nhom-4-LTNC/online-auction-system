package com.auction.server.controller;

import com.auction.server.handler.ClientHandler;
import com.auction.server.service.AuctionChatService;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.bid.SendAuctionChatRequest;
import com.auction.shared.protocol.bid.SendAuctionChatResponse;

public class AuctionChatController {

    private final AuctionChatService auctionChatService = AuctionChatService.getInstance();

    public Response<?> handleSendChat(Request<?> request, ClientHandler client) {
        try {
            if (client == null || client.getCurrentUser() == null) {
                return Response.error(ActionType.CHAT_MESSAGE, "Bạn cần đăng nhập để gửi tin nhắn.");
            }

            if (request == null || !(request.getPayload() instanceof SendAuctionChatRequest payload)) {
                return Response.error(ActionType.CHAT_MESSAGE, "Payload gửi tin nhắn không hợp lệ.");
            }

            auctionChatService.broadcastChatMessage(
                    payload.getAuctionId(),
                    client,
                    payload.getMessage(),
                    false
            );

            return Response.success(ActionType.CHAT_MESSAGE, new SendAuctionChatResponse("Đã gửi tin nhắn."));
        } catch (Exception e) {
            return Response.error(ActionType.CHAT_MESSAGE, e.getMessage() == null ? "Gửi tin nhắn thất bại." : e.getMessage());
        }
    }
}

