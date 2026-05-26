package com.auction.server.controller;

import com.auction.server.event.AuctionEventPublisher;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.server.model.Bid;
import com.auction.server.model.auction.Auction;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.bid.*;
import com.auction.server.handler.ClientHandler;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;

import java.util.List;

public class BidController {

    private final BidService bidService = BidService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionEventPublisher auctionEventPublisher =
            AuctionEventPublisher.getInstance();

    public Response<PlaceBidResponse> handlePlaceBid(Request<?> request, ClientHandler client) {
        try {
            Object payload = request.getPayload();

            if (!(payload instanceof PlaceBidRequest placeBidRequest)) {
                return Response.error(
                        ActionType.PLACE_BID,
                        "Payload đặt giá không hợp lệ."
                );
            }
            if (!client.isLoggedIn()) {
                return Response.error(ActionType.PLACE_BID,
                        "Người dùng chưa đăng nhập.");
            }

            Auction updatedAuction = bidService.placeBid(
                    client.getCurrentUser().getId(),
                    placeBidRequest.getAuctionId(),
                    placeBidRequest.getAmount()
            );
            AuctionDetailDTO auctionDetailDTO = auctionService.getAuctionDetail(updatedAuction.getId());
            AuctionSummaryDTO summary =
                    auctionService.mapToAuctionSummaryDTO(updatedAuction);


            AuctionUpdatedEvent event = new AuctionUpdatedEvent(
                    updatedAuction.getId(),
                    AuctionUpdateType.BID_PLACED,
                    summary,
                    null,
                    "Có lượt đặt giá mới.",
                    System.currentTimeMillis()
            );

            auctionEventPublisher.publishAuctionUpdated(event);

            return Response.success(
                    ActionType.PLACE_BID,
                    new PlaceBidResponse(auctionDetailDTO,
                            "Đặt giá thành công!")
            );

        } catch (AuctionAppException e) {
            return Response.error(ActionType.PLACE_BID, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.PLACE_BID, "Lỗi không xác định khi đặt giá.");
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
            if (!client.isLoggedIn()) {
                return Response.error(ActionType.GET_BIDS_BY_AUCTION,
                        "Người dùng chưa đăng nhập.");
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
            if (!client.isLoggedIn()) {
                return Response.error(ActionType.GET_BIDS_BY_BIDDER, "Người dùng chưa đăng nhập.");
            }

            List<Bid> bids = bidService.getBidsByBidderForRequester(
                    client.getCurrentUser().getId(),
                    getBidsByBidderRequest.getBidderId()
            );
            List<BidDTO> bidDTOs = bidService.mapToBidDTOList(bids);

            return Response.success(
                    ActionType.GET_BIDS_BY_BIDDER,
                    new GetBidHistoryResponse(bidDTOs,
                            "Lấy lịch sử bid theo người tham gia thành công!")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(
                    ActionType.GET_BIDS_BY_BIDDER,
                    "Lỗi không xác định khi lấy lịch sử bid theo người tham gia."
            );
        }
    }

    public Response <GetBidHistoryResponse> handleGetCurrentUserBids(ClientHandler client) throws Exception {
        if (!client.isLoggedIn()) {
            return Response.error(ActionType.GET_MY_BIDS,
                    "Người dùng chưa đăng nhập.");
        }
        List <Bid> bids = bidService.getBidsByBidder(client.getCurrentUser().getId());
        List <BidDTO> bidDTOs = null;
        try {
            bidDTOs = bidService.mapToBidDTOList(bids);
        } catch (AuctionAppException e) {
            throw new RuntimeException(e);
        }

        return Response.success(
                    ActionType.GET_MY_BIDS,
                    new GetBidHistoryResponse(bidDTOs,
                            "Lấy lịch sử bid thành công!")
            );
    }
}
