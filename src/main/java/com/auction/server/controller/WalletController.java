package com.auction.server.controller;

import com.auction.server.handler.ClientHandler;
import com.auction.server.service.PaymentService;
import com.auction.server.service.WalletService;
import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import com.auction.shared.protocol.finance.PayAuctionRequest;

public class WalletController {
    private final WalletService walletService = WalletService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();

    public Response<BalanceResponse> handleAddBalance(ClientHandler clientHandler, Request<?> request) {
        try {
            if (clientHandler.getCurrentUser() == null) {
                return Response.error(ActionType.ADD_BALANCE, "Bạn cần đăng nhập để nạp tiền.");
            }

            Object payload = request.getPayload();
            if (!(payload instanceof AddBalanceRequest addBalanceRequest)) {
                return Response.error(ActionType.ADD_BALANCE, "Payload nạp tiền không hợp lệ.");
            }

            int userId = clientHandler.getCurrentUser().getId();
            BalanceResponse response = walletService.addBalance(userId, addBalanceRequest.getAmount());
            return Response.success(ActionType.ADD_BALANCE, response);
        } catch (Exception e) {
            return Response.error(ActionType.ADD_BALANCE, e.getMessage());
        }
    }

    public Response<PayAuctionResponse> handlePayAuction(ClientHandler clientHandler, Request<?> request) {
        try {
            if (clientHandler.getCurrentUser() == null) {
                return Response.error(ActionType.PAY_AUCTION, "Bạn cần đăng nhập để thanh toán.");
            }

            Object payload = request.getPayload();
            if (!(payload instanceof PayAuctionRequest payAuctionRequest)) {
                return Response.error(ActionType.PAY_AUCTION, "Payload thanh toán không hợp lệ.");
            }

            int payerId = clientHandler.getCurrentUser().getId();
            PayAuctionResponse response = paymentService.payAuction(payerId, payAuctionRequest.getAuctionId());
            return Response.success(ActionType.PAY_AUCTION, response);
        } catch (Exception e) {
            return Response.error(ActionType.PAY_AUCTION, e.getMessage());
        }
    }
}
