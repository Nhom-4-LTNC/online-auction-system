package com.auction.server.controller;

import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.*;
import com.auction.server.handler.ClientHandler;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;

import java.util.List;

/**
 * Controller chịu trách nhiệm xử lý các luồng nghiệp vụ Đấu giá
 * (Tạo phòng, Xem danh sách, Đóng phòng).
 */
public class AuctionController {

    private final AuctionService auctionService = AuctionService.getInstance();
    private final BidService bidService = BidService.getInstance();
    /**
     * Lấy danh sách tóm tắt tất cả các phòng đấu giá hiện có.
     * Hành động này có thể thực hiện bởi cả người dùng chưa đăng nhập (Guest).
     *
     * @param request Gói tin Request rỗng (không cần payload).
     * @param client  Tham chiếu đến luồng ClientHandler.
     * @return Response chứa danh sách {@code List<AuctionSummaryDTO>}.
     */
    public Response<?> handleGetAllAuctions(Request<?> request, ClientHandler client) {
        try {
            List<AuctionSummaryDTO> summaries = auctionService.getAllAuctions();
            return new Response<>(ActionType.GET_ALL_AUCTIONS,
                    new GetAllAuctionResponse(summaries, "Lấy danh sách phòng đấu giá thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>(ActionType.GET_ALL_AUCTIONS, "Lỗi máy chủ khi lấy danh sách phòng!");
        }
    }

    /**
     * Lấy thông tin chi tiết của một phòng đấu giá cụ thể dựa trên ID.
     *
     * @param request Gói tin chứa GetAuctionRequest (chứa auctionId).
     * @param client  Tham chiếu đến luồng ClientHandler.
     * @return Response chứa đối tượng {@code AuctionDetailDTO}.
     */
    public Response<?> handleGetAuction(Request<?> request, ClientHandler client) {
        try {
            GetAuctionRequest req = (GetAuctionRequest) request.getPayload();
            AuctionDetailDTO detailDTO = auctionService.getAuctionDetail(req.getAuctionId());
            List <BidDTO> recentBids = bidService.mapToBidDTOList(bidService.getBidsByAuctionId(req.getAuctionId()));
            return Response.success(ActionType.GET_AUCTION,
                    new GetAuctionResponse(detailDTO, recentBids, null));

        } catch (AuctionAppException e) {
            // Bắt lỗi: ResourceNotFoundException (Không tìm thấy phòng)
            return new Response<>(ActionType.GET_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>(ActionType.GET_AUCTION, "Lỗi máy chủ khi lấy chi tiết phòng đấu giá!");
        }
    }

    /**
     * Xử lý yêu cầu tạo phòng đấu giá mới từ Client.
     * Bắt buộc người dùng phải đăng nhập mới được phép thực hiện.
     *
     * @param request Gói tin chứa CreateAuctionRequest.
     * @param client  Tham chiếu đến luồng ClientHandler để xác thực.
     * @return Response chứa thông báo thành công hoặc lỗi.
     */
    public Response<?> handleCreateAuction(Request<?> request, ClientHandler client) {
        try {
            // 1. Phân quyền: Kiểm tra xem đã đăng nhập chưa
            if (client.getCurrentUser() == null) {
                return new Response<>(ActionType.CREATE_AUCTION, "Bạn phải đăng nhập để tạo phòng đấu giá!");
            }

            CreateAuctionRequest req = (CreateAuctionRequest) request.getPayload();

            // 2. Lấy ID của chính người đang yêu cầu để gán làm Chủ phòng (Owner/Seller)
            int sellerId = client.getCurrentUser().getId();

            // 3. Gọi Service để xử lý tạo phòng
            auctionService.createAuction(sellerId, req.getItemDto(), req.getBidStep(), req.getEndTime());

            return new Response<>(ActionType.CREATE_AUCTION, "Tạo phòng đấu giá thành công!");

        } catch (AuctionAppException e) {
            return new Response<>(ActionType.CREATE_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>(ActionType.CREATE_AUCTION,"Lỗi máy chủ khi tạo phòng đấu giá!");
        }
    }

    /**
     * Xử lý yêu cầu đóng phòng đấu giá (kết thúc sớm).
     * Bắt buộc phải đăng nhập. Service sẽ lo việc kiểm tra xem User này có phải chủ phòng hay Admin không.
     *
     * @param request Gói tin chứa CloseAuctionRequest.
     * @param client  Tham chiếu đến luồng ClientHandler.
     * @return Response thông báo kết quả.
     */
    public Response<?> handleCloseAuction(Request<?> request, ClientHandler client) {
        try {
            if (client.getCurrentUser() == null) {
                return Response.error(ActionType.CLOSE_AUCTION,"Bạn phải đăng nhập để thực hiện thao tác này!");
            }

            CloseAuctionRequest req = (CloseAuctionRequest) request.getPayload();
            int requesterId = client.getCurrentUser().getId();

            // Service sẽ ném lỗi nếu requesterId không có quyền đóng phòng này
            auctionService.closeAuction(requesterId, req.getAuctionId());

            return Response.success(ActionType.CLOSE_AUCTION, "Đóng phiên đấu giá thành công!");
        } catch (AuctionAppException e) {
            // Bắt lỗi: ActionNotAllowedException (Không có quyền), AuctionClosedException (Phòng đã đóng sẵn)
            return Response.error(ActionType.CLOSE_AUCTION, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.CLOSE_AUCTION,"Lỗi máy chủ khi đóng phòng!");
        }
    }
}