package com.auction.server.controller;

import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.handler.ClientHandler;
import com.auction.server.model.auction.Auction;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.*;

import java.util.List;

public class AuctionController {

    private final AuctionService auctionService = AuctionService.getInstance();
    private final BidService bidService = BidService.getInstance();
    private final AuctionEventPublisher auctionEventPublisher = AuctionEventPublisher.getInstance();

    public Response<?> handleGetAllAuctions() {
        try {
            List<AuctionSummaryDTO> summaries = auctionService.getAllAuctions();
            return Response.success(
                    ActionType.GET_ALL_AUCTIONS,
                    new GetAllAuctionResponse(summaries, "Lấy danh sách phòng đấu giá thành công")
            );
        } catch (Exception e) {
            logUnexpected(ActionType.GET_ALL_AUCTIONS, e);
            return Response.error(ActionType.GET_ALL_AUCTIONS, "Lỗi máy chủ khi lấy danh sách phòng!");
        }
    }

    public Response<?> handleGetAuction(Request<?> request, ClientHandler client) {
        try {
            GetAuctionRequest req = (GetAuctionRequest) request.getPayload();
            AuctionDetailDTO detailDTO = auctionService.getAuctionDetail(req.getAuctionId());
            List<BidDTO> recentBids = bidService.getRecentBidHistoryByAuction(req.getAuctionId());
            return Response.success(
                    ActionType.GET_AUCTION,
                    new GetAuctionResponse(detailDTO, recentBids, "Lấy thông tin phiên đấu giá thành công")
            );
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_AUCTION, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.GET_AUCTION, e);
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
                    req.getStartTime(),
                    req.getEndTime()
            );
            AuctionDetailDTO createdAuctionDetail = auctionService.mapToAuctionDetailDTO(createdAuction);
            AuctionSummaryDTO summary = auctionService.mapToAuctionSummaryDTO(createdAuction);
            auctionEventPublisher.publishAuctionUpdatedExcept(
                    createdAuction.getId(),
                    AuctionUpdateType.AUCTION_CREATED,
                    summary,
                    null,
                    "Co phien dau gia moi duoc tao.",
                    client
            );

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
            logUnexpected(ActionType.CREATE_AUCTION, e);
            return Response.error(ActionType.CREATE_AUCTION, "Lỗi máy chủ khi tạo phòng đấu giá!");
        }
    }
    public Response<?> handleGetAuctionsByType(Request<?> request) {
        try {
            Object payload = request.getPayload();

            if (!(payload instanceof GetAuctionsByTypeRequest getRequest)) {
                return Response.error(
                        ActionType.GET_AUCTIONS_BY_TYPE,
                        "Payload GET_AUCTIONS_BY_TYPE không hợp lệ."
                );
            }

            List<AuctionSummaryDTO> auctions =
                    auctionService.getAuctionSummariesByType(getRequest.getItemType());
            GetAuctionsByTypeResponse responsePayload =
                    new GetAuctionsByTypeResponse(
                            auctions,
                            "Lấy danh sách đấu giá theo loại thành công."
                    );

            return Response.success(ActionType.GET_AUCTIONS_BY_TYPE, responsePayload);

        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_AUCTIONS_BY_TYPE, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.GET_AUCTIONS_BY_TYPE, e);
            return Response.error(
                    ActionType.GET_AUCTIONS_BY_TYPE,
                    "Lỗi máy chủ khi lấy danh sách đấu giá theo loại."
            );
        }
    }

    public Response<?> handleGetMyCreatedAuctions(ClientHandler client) {
        try {
            if (client == null || client.getCurrentUser() == null) {
                return Response.error(
                        ActionType.GET_MY_CREATED_AUCTIONS,
                        "Người dùng chưa đăng nhập."
                );
            }

            List<AuctionSummaryDTO> auctions =
                    auctionService.getAuctionsCreatedByUser(client.getCurrentUser().getId());

            return Response.success(ActionType.GET_MY_CREATED_AUCTIONS, auctions);
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_MY_CREATED_AUCTIONS, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.GET_MY_CREATED_AUCTIONS, e);
            return Response.error(
                    ActionType.GET_MY_CREATED_AUCTIONS,
                    "Lỗi máy chủ khi lấy danh sách phiên đấu giá đã tạo."
            );
        }
    }

    public Response<?> handleGetMyParticipatedAuctions(ClientHandler client) {
        try {
            if (client == null || client.getCurrentUser() == null) {
                return Response.error(
                        ActionType.GET_MY_PARTICIPATED_AUCTIONS,
                        "Người dùng chưa đăng nhập."
                );
            }

            List<AuctionSummaryDTO> auctions =
                    auctionService.getAuctionsParticipatedByUser(client.getCurrentUser().getId());

            return Response.success(ActionType.GET_MY_PARTICIPATED_AUCTIONS, auctions);
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_MY_PARTICIPATED_AUCTIONS, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.GET_MY_PARTICIPATED_AUCTIONS, e);
            return Response.error(
                    ActionType.GET_MY_PARTICIPATED_AUCTIONS,
                    "Lỗi máy chủ khi lấy danh sách phiên đấu giá đã tham gia."
            );
        }
    }

    public Response<?> handleGetMyWonAuctions(ClientHandler client) {
        try {
            if (client == null || client.getCurrentUser() == null) {
                return Response.error(
                        ActionType.GET_MY_WON_AUCTIONS,
                        "Người dùng chưa đăng nhập."
                );
            }

            List<AuctionSummaryDTO> auctions =
                    auctionService.getAuctionsWonByUser(client.getCurrentUser().getId());

            return Response.success(ActionType.GET_MY_WON_AUCTIONS, auctions);
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_MY_WON_AUCTIONS, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.GET_MY_WON_AUCTIONS, e);
            return Response.error(
                    ActionType.GET_MY_WON_AUCTIONS,
                    "Lỗi máy chủ khi lấy danh sách phiên đấu giá đã thắng."
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
            Auction closedAuction = auctionService.getAuctionModelById(req.getAuctionId());
            AuctionSummaryDTO summary = auctionService.mapToAuctionSummaryDTO(closedAuction);
            auctionEventPublisher.publishAuctionUpdated(
                    req.getAuctionId(),
                    AuctionUpdateType.AUCTION_CLOSED,
                    summary,
                    null,
                    "Phiên đấu giá đã được đóng."
            );

            return Response.success(ActionType.CLOSE_AUCTION, "Đóng phiên đấu giá thành công!");
        } catch (AuctionAppException e) {
            return Response.error(ActionType.CLOSE_AUCTION, e.getMessage());
        } catch (Exception e) {
            logUnexpected(ActionType.CLOSE_AUCTION, e);
            return Response.error(ActionType.CLOSE_AUCTION, "Lỗi máy chủ khi đóng phòng!");
        }
    }

    private void logUnexpected(ActionType actionType, Exception e) {
        System.err.println("[AuctionController] Unexpected error action=" + actionType
                + ": " + e.getMessage());
    }
}
