package com.auction.server.controller;

import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.server.model.Bid;
import com.auction.server.model.auction.Auction;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.bid.*;
import com.auction.server.network.ClientHandler;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;

import java.util.List;

public class BidController {

    private final BidService bidService = BidService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();

    public Response<PlaceBidResponse> handlePlaceBid(Request<?> request, ClientHandler client) {
        try {
            Object payload = request.getPayload();

            if (!(payload instanceof PlaceBidRequest placeBidRequest)) {
                return Response.error(
                        ActionType.PLACE_BID,
                        "Payload đặt giá không hợp lệ."
                );
            }

            Auction updatedAuction = bidService.placeBid(
                    client.getCurrentUser(),
                    placeBidRequest
            );
            AuctionDetailDTO auctionDetailDTO = auctionService.getAuctionDetail(updatedAuction.getId());

            return Response.success(
                    ActionType.PLACE_BID,
                    new PlaceBidResponse(auctionDetailDTO,
                            "Đặt giá thành công!")
            );
        } catch (Exception e) {
            return Response.error(
                    ActionType.PLACE_BID,
                    "Lỗi khi đặt giá: " + e.getMessage()
            );
        }
    }

    public Response<GetBidHistoryResponse> handleGetBidsByAuction(Request<?> request, ClientHandler client) {
        try {
            Object payload = request.getPayload();
            if (!(payload instanceof GetBidsByAuctionRequest getBidHistoryRequest)) {
                return Response.error(
                        ActionType.GET_BIDS_BY_AUCTION,
                        "Payload không hợp lệ."
                );
            }

            List<Bid> bids = bidService.getBidsByAuctionId(getBidHistoryRequest.getAuctionId());
            List <BidDTO> bidDTOs = bidService.mapToBidDTOList(bids);

            return Response.success(
                    ActionType.GET_BIDS_BY_AUCTION,
                    new GetBidHistoryResponse(bidDTOs,
                            "Lấy lịch sử bid theo phiên đấu giá thành công!")
            );
        } catch (Exception e) {
            return Response.error(
                    ActionType.GET_BIDS_BY_AUCTION,
                    e.getMessage()
            );
        }
    }

    public Response<GetBidHistoryResponse> handleGetBidsByBidder(Request<?> request,
                                                                 ClientHandler client) {
        try {
            Object payload = request.getPayload();
            if (!(payload instanceof GetBidsByBidderRequest getBidsByBidderRequest)) {
                return Response.error(
                        ActionType.GET_BIDS_BY_BIDDER,
                        "Payload không hợp lệ!"
                );
            }

            List<Bid> bids = bidService.getBidsByBidder(getBidsByBidderRequest.getBidderId());
            List<BidDTO> bidDTOs = bidService.mapToBidDTOList(bids);

            return Response.success(
                    ActionType.GET_BIDS_BY_BIDDER,
                    new GetBidHistoryResponse(bidDTOs,
                            "Lấy lịch sử bid theo người tham gia thành công!")
            );
        } catch (Exception e) {
            return Response.error(
                    ActionType.GET_BIDS_BY_BIDDER,
                    e.getMessage()
            );
        }
    }

    public Response <GetBidHistoryResponse> handleGetCurrentUserBids(ClientHandler client) {
        List <Bid> bids = bidService.getBidsByBidder(client.getCurrentUser().getId());
        List <BidDTO> bidDTOs = bidService.mapToBidDTOList(bids);

        return Response.success(
                    ActionType.GET_MY_BIDS,
                    new GetBidHistoryResponse(bidDTOs,
                            "Lấy lịch sử bid thành công!")
            );
    }
}
