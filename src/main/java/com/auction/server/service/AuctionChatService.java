package com.auction.server.service;

import com.auction.server.Server;
import com.auction.server.handler.ClientHandler;
import com.auction.server.model.auction.Auction;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.event.AuctionChatMessageEvent;

/**
 * Chat realtime-only giữa các user trong cùng auction.
 * (Chưa lưu DB, theo TODO.md Phase 1)
 */
public class AuctionChatService {

    private static volatile AuctionChatService instance;

    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();

    private AuctionChatService() {}

    public static AuctionChatService getInstance() {
        if (instance == null) {
            synchronized (AuctionChatService.class) {
                if (instance == null) {
                    instance = new AuctionChatService();
                }
            }
        }
        return instance;
    }

    public void broadcastChatMessage(int auctionId, ClientHandler sender, String message, boolean isSystem) {
        if (sender == null || sender.getCurrentUser() == null) {
            // không broadcast nếu chưa đăng nhập
            return;
        }


        if (message == null) {
            message = "";
        }
        String trimmed = message.trim();
        if (trimmed.isBlank()) {
            return;
        }

        if (trimmed.length() > 1000) {
            trimmed = trimmed.substring(0, 1000);
        }

        Auction auction;
        try {
            auction = auctionService.getAuctionModelById(auctionId);
        } catch (Exception e) {
            // nếu auction không tồn tại => bỏ qua broadcast
            return;
        }

        AuctionStatus status = auction == null ? null : auction.getStatus();
        if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            // chỉ cho chat khi phiên còn mở/chạy
            return;
        }

        UserDTO u = sender.getCurrentUser();
        String username = u.getUsername();

        AuctionChatMessageEvent event = new AuctionChatMessageEvent(
                auctionId,
                u.getId(),
                username,
                trimmed,
                System.currentTimeMillis(),
                isSystem
        );

        Response<AuctionChatMessageEvent> response = Response.success(
                ActionType.CHAT_MESSAGE,
                event
        );

        // Phase 1: broadcast tạm thời tới toàn bộ user đã login.
        Server.broadcastToLoggedIn(response);
    }
}

