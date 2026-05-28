package com.auction.client.service;

import com.auction.shared.dto.BidDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.bid.GetBidHistoryResponse;
import com.auction.shared.protocol.bid.GetBidsByAuctionRequest;
import com.auction.shared.protocol.bid.PlaceBidRequest;
import com.auction.shared.protocol.bid.PlaceBidResponse;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class BidClientService extends BaseClientService {

    public List<BidDTO> getBidHistoryByAuction(int auctionId) {
        Request<GetBidsByAuctionRequest> request = new Request<>(
                ActionType.GET_BIDS_BY_AUCTION,
                new GetBidsByAuctionRequest(auctionId)
        );

        GetBidHistoryResponse response = sendAndExtract(request, GetBidHistoryResponse.class);
        if (response == null || response.getBidHistory() == null) {
            return Collections.emptyList();
        }
        return response.getBidHistory();
    }

    public PlaceBidResponse placeBid(int auctionId, BigDecimal amount) {
        Request<PlaceBidRequest> request = new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, amount.doubleValue())
        );

        return sendAndExtract(request, PlaceBidResponse.class);
    }
}
