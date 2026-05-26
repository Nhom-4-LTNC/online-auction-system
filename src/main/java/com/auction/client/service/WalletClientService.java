package com.auction.client.service;

import com.auction.client.network.Client;
import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import com.auction.shared.protocol.finance.PayAuctionRequest;

public class WalletClientService {
    private static final long DEFAULT_TIMEOUT_MILLIS = 10_000L;

    private final Client client = Client.getInstance();

    public BalanceResponse addBalance(double amount) throws Exception {
        Request<AddBalanceRequest> request = new Request<>(
                ActionType.ADD_BALANCE,
                new AddBalanceRequest(amount)
        );

        Response<?> response = client.sendRequestAndWait(request, DEFAULT_TIMEOUT_MILLIS);
        if (!response.isSuccess()) {
            throw new Exception(response.getErrorMessage());
        }
        if (!(response.getPayload() instanceof BalanceResponse balanceResponse)) {
            throw new Exception("Phản hồi nạp tiền không đúng định dạng.");
        }
        return balanceResponse;
    }

    public PayAuctionResponse payAuction(int auctionId) throws Exception {
        Request<PayAuctionRequest> request = new Request<>(
                ActionType.PAY_AUCTION,
                new PayAuctionRequest(auctionId)
        );

        Response<?> response = client.sendRequestAndWait(request, DEFAULT_TIMEOUT_MILLIS);
        if (!response.isSuccess()) {
            throw new Exception(response.getErrorMessage());
        }
        if (!(response.getPayload() instanceof PayAuctionResponse payAuctionResponse)) {
            throw new Exception("Phản hồi thanh toán không đúng định dạng.");
        }
        return payAuctionResponse;
    }
}
