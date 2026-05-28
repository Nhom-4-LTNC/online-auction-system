package com.auction.server.controller;

import com.auction.server.handler.ClientHandler;
import com.auction.server.model.auction.Auction;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.*;

import java.util.List;

public class AuctionController {

    private final AuctionService auctionService = AuctionService.getInstance();
    private final BidService bidService = BidService.getInstance();

    public Response<?> handleGetAllAuctions() {
        try {
            System.out.println("[AuctionController] Received GET_ALL_AUCTIONS");
            List<AuctionSummaryDTO> summaries = auctionService.getAllAuctions();
            System.out.println("[AuctionController] GET_ALL_AUCTIONS success, count=" + summaries.size());
            return Response.success(
                    ActionType.GET_ALL_AUCTIONS,
                    new GetAllAuctionResponse(summaries, "Lấy danh sách phòng đấu giá thành công")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.GET_ALL_AUCTIONS, "Lỗi máy chủ khi lấy danh sách phòng!");
        }
    }

    public Response<?> handleGetAuction(Request<?> request, ClientHandler client) {
        try {
            GetAuctionRequest req = (GetAuctionRequest) request.getPayload();
            AuctionDetailDTO detailDTO = auctionService.getAuctionDetail(req.getAuctionId());
            List<BidDTO> recentBids = bidService.mapToBidDTOList(
                    bidService.getBidsByAuctionId(req.getAuctionId())
            );
            return Response.success(
                    ActionType.GET_AUCTION,
                    new GetAuctionResponse(detailDTO, recentBids, "Lấy thông tin phiên đấu giá thành công")
            );
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.GET_AUCTION, "Lỗi máy chủ khi lấy chi tiết phòng đấu giá!");
        }
    }

    public Response<?> handleCreateAuction(Request<?> request, ClientHandler client) {
        try {
            if (client.getCurrentUser() == null) {
                return Response.error(
                        ActionType.CREATE_AUCTION,
                        "Bạn phải đăng nhập để tạo phòng đấu giá!"
                );
            }

            CreateAuctionRequest req = (CreateAuctionRequest) request.getPayload();
            int sellerId = client.getCurrentUser().getId();

            Auction createdAuction = auctionService.createAuction(
                    sellerId,
                    req.getItemDto(),
                    req.getBidStep(),
                    req.getEndTime()
            );
            AuctionDetailDTO createdAuctionDetail = auctionService.mapToAuctionDetailDTO(createdAuction);

            return Response.success(
                    ActionType.CREATE_AUCTION,
                    new CreateAuctionResponse(
                            createdAuctionDetail,
                            "Tạo phòng đấu giá thành công!"
                    )
            );
        } catch (AuctionAppException e) {
            return Response.error(ActionType.CREATE_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.CREATE_AUCTION, "Lỗi máy chủ khi tạo phòng đấu giá!");
        }
    }
    public Response<?> handleGetAuctionsByType(Request<?> request) {
        try {
            System.out.println("[AuctionController] Received GET_AUCTIONS_BY_TYPE");
            Object payload = request.getPayload();

            if (!(payload instanceof GetAuctionsByTypeRequest getRequest)) {
                return Response.error(
                        ActionType.GET_AUCTIONS_BY_TYPE,
                        "Payload GET_AUCTIONS_BY_TYPE không hợp lệ."
                );
            }

            List<AuctionSummaryDTO> auctions =
                    auctionService.getAuctionSummariesByType(getRequest.getItemType());
            System.out.println("[AuctionController] GET_AUCTIONS_BY_TYPE success, type="
                    + getRequest.getItemType() + ", count=" + auctions.size());

            GetAuctionsByTypeResponse responsePayload =
                    new GetAuctionsByTypeResponse(
                            auctions,
                            "Lấy danh sách đấu giá theo loại thành công."
                    );

            return Response.success(ActionType.GET_AUCTIONS_BY_TYPE, responsePayload);

        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_AUCTIONS_BY_TYPE, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(
                    ActionType.GET_AUCTIONS_BY_TYPE,
                    "Lỗi máy chủ khi lấy danh sách đấu giá theo loại."
            );
        }
    }
    public Response<?> handleCloseAuction(Request<?> request, ClientHandler client) {
        try {
            if (client.getCurrentUser() == null) {
                return Response.error(
                        ActionType.CLOSE_AUCTION,
                        "Bạn phải đăng nhập để thực hiện thao tác này!"
                );
            }

            CloseAuctionRequest req = (CloseAuctionRequest) request.getPayload();
            int requesterId = client.getCurrentUser().getId();

            auctionService.closeAuction(requesterId, req.getAuctionId());

            return Response.success(ActionType.CLOSE_AUCTION, "Đóng phiên đấu giá thành công!");
        } catch (AuctionAppException e) {
            return Response.error(ActionType.CLOSE_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.CLOSE_AUCTION, "Lỗi máy chủ khi đóng phòng!");
        }
    }
}
