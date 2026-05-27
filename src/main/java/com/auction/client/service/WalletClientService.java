package com.auction.client.service;

import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import com.auction.shared.protocol.finance.PayAuctionRequest;

public class WalletClientService extends BaseClientService {

    public BalanceResponse addBalance(double amount) {
        Request<AddBalanceRequest> request = new Request<>(
                ActionType.ADD_BALANCE,
                new AddBalanceRequest(amount)
        );

        return sendAndExtract(request, BalanceResponse.class);
    }

    public PayAuctionResponse payAuction(int auctionId) {
        Request<PayAuctionRequest> request = new Request<>(
                ActionType.PAY_AUCTION,
                new PayAuctionRequest(auctionId)
        );

        return sendAndExtract(request, PayAuctionResponse.class);
    }
}
