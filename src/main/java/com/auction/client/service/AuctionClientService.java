package com.auction.client.service;

import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.ItemType;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.protocol.auction.GetAuctionsByTypeRequest;
import com.auction.shared.protocol.auction.GetAuctionsByTypeResponse;

import java.util.Collections;
import java.util.List;

public class AuctionClientService extends BaseClientService {

    public CreateAuctionResponse createAuction(CreateAuctionRequest createAuctionRequest) {
        Request<CreateAuctionRequest> request = new Request<>(
                ActionType.CREATE_AUCTION,
                createAuctionRequest
        );

        return sendAndExtract(request, CreateAuctionResponse.class);
    }

    public List<AuctionSummaryDTO> getAuctionsByType(ItemType type) {
        Request<GetAuctionsByTypeRequest> request = new Request<>(
                ActionType.GET_AUCTIONS_BY_TYPE,
                new GetAuctionsByTypeRequest(type)
        );

        GetAuctionsByTypeResponse response = sendAndExtract(request, GetAuctionsByTypeResponse.class);
        if (response == null || response.getAuctions() == null) {
            return Collections.emptyList();
        }
        return response.getAuctions();
    }
}
