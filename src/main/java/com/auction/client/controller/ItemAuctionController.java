package com.auction.client.controller;

import java.io.IOException;

import com.auction.client.network.Client;
import com.auction.shared.util.SceneUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ItemAuctionController {

    /*
     * REVIEW - VẤN ĐỀ KIẾN TRÚC NGHIÊM TRỌNG:
     *
     * Đây là controller phía client, không được gọi trực tiếp AuctionService phía server.
     *
     * Nếu AuctionService ở đây là:
     *
     * com.auction.server.service.AuctionService
     *
     * thì flow hiện tại đang là:
     *
     * JavaFX Controller
     *   -> server.service.AuctionService
     *   -> Repository
     *   -> Database
     *
     * Đây là phá kiến trúc Client-Server.
     *
     * Flow đúng phải là:
     *
     * JavaFX Controller
     *   -> AuctionClientService/BidClientService phía client
     *   -> gửi Request qua socket
     *   -> Server ClientHandler
     *   -> AuctionController/BidController phía server
     *   -> AuctionService/BidService phía server
     *   -> Repository
     *   -> Database
     *
     * Hướng sửa:
     * - Xóa dependency tới AuctionService phía server.
     * - Tạo client-side service:
     *   + AuctionClientService.getAuctionDetail(auctionId)
     *   + BidClientService.placeBid(auctionId, amount)
     * - Các client service này gửi Request/Response đúng protocol hiện tại.
     */
    private final AuctionService auctionService = AuctionService.getInstance();

    @FXML private Label titleLabel;
    @FXML private Label descLabel;
    @FXML private Label miscLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label endDateLabel;
    @FXML private Label notificationText;

    @FXML private TextField bidTextField;
    @FXML private Button bidButton;

    @FXML
    private void OutAuction(ActionEvent event) throws IOException {
        /*
         * REVIEW - PROTOCOL SAI:
         *
         * SubscribeAuctionRequest không thuộc protocol hiện tại của hệ thống.
         * Bạn đang tự thêm một message riêng:
         *
         * new SubscribeAuctionRequest(auctionId, false)
         *
         * trong khi protocol chuẩn của project đang đi theo dạng:
         *
         * Request<ActionType, payload>
         * Response<ActionType, status, payload/errorMessage>
         *
         * Nếu muốn subscribe realtime, cần thêm chính thức vào protocol:
         *
         * ActionType.SUBSCRIBE_AUCTION
         * ActionType.UNSUBSCRIBE_AUCTION
         *
         * Payload có thể là:
         *
         * AuctionSubscriptionRequest {
         *     int auctionId;
         * }
         *
         * Không nên tạo request tự do không được ClientHandler/Dispatcher xử lý.
         *
         * Ngoài ra, nếu chỉ đang làm demo cơ bản, có thể bỏ subscribe/unsubscribe trước,
         * dùng nút refresh thủ công để giảm phức tạp.
         */
        try {
            Client client = Client.getInstance();
            if (client.isConnected() && auctionId != null) {
                client.sendMessage(new SubscribeAuctionRequest(auctionId, false));
            }
        } catch (Exception ignore) {
            /*
             * REVIEW:
             * Không nên nuốt exception im lặng.
             * Ít nhất nên log:
             *
             * e.printStackTrace();
             *
             * Nhưng tốt hơn là chưa làm realtime nếu protocol chưa sẵn sàng.
             */
        }

        SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
    }

    private Integer auctionId;

    /**
     * Set auctionId + update title with current product info.
     */
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;

        String fallbackTitle = "AUCTION #" + auctionId;

        try {
            /*
             * REVIEW - SAI KIẾN TRÚC:
             *
             * Client không được gọi auctionService.getAuctionById(...) của server.
             *
             * Cách đúng:
             *
             * AuctionDetailDTO detail = auctionClientService.getAuctionDetail(auctionId);
             *
             * Trong đó AuctionClientService gửi:
             *
             * Request<GetAuctionRequest>
             *
             * tới server qua socket, server trả:
             *
             * Response<GetAuctionResponse hoặc AuctionDetailDTO>
             *
             * Client chỉ nên nhận DTO, không nên nhận model Auction.
             */
            Auction auction = auctionService.getAuctionById(auctionId);

            if (auction == null) {
                setTitle(fallbackTitle);
                return;
            }

            /*
             * REVIEW - CLIENT ĐANG DÙNG SERVER MODEL:
             *
             * Auction và Item ở đây có vẻ là model phía server:
             *
             * com.auction.server.model.auction.Auction
             * com.auction.server.model.item.Item
             *
             * Client không nên phụ thuộc vào server model.
             *
             * Cách đúng:
             * - Server map Auction -> AuctionDetailDTO.
             * - Client dùng AuctionDetailDTO để hiển thị.
             */
            Item item = auction.getItem();

            if (item == null || item.getName() == null || item.getName().isBlank()) {
                setTitle(fallbackTitle);
                return;
            }

            /*
             * REVIEW - SO SÁNH USER SAI:
             *
             * item.getOwner() != SessionManager.getInstance().getCurrentUser()
             *
             * Đây là so sánh reference object, không phải so sánh cùng user thật.
             * Hai object User có cùng id nhưng khác instance thì != vẫn true.
             *
             * Nếu cần kiểm tra chủ sản phẩm, hãy so sánh id:
             *
             * currentUser != null && itemOwnerId == currentUser.getId()
             *
             * Nhưng tốt hơn: server trả DTO có ownerId, client so sánh ownerId với currentUser.id.
             */
            bidTextField.setVisible(item.getOwner() != SessionManager.getInstance().getCurrentUser());
            bidButton.setVisible(item.getOwner() != SessionManager.getInstance().getCurrentUser());

            String currentPrice = String.format("%.2f", auction.getCurrentPrice());
            long endTime = auction.getEndTime();

            String endsText = "";
            try {
                var dt = java.time.Instant.ofEpochMilli(endTime);
                var ldt = java.time.LocalDateTime.ofInstant(dt, java.time.ZoneId.systemDefault());
                endsText = ldt.toLocalTime().toString();
            } catch (Exception ignore) {
                /*
                 * REVIEW:
                 * Không nên nuốt lỗi format time im lặng nếu đang debug UI.
                 * Có thể tạm chấp nhận cho demo, nhưng nên log nếu dữ liệu thời gian sai.
                 */
            }

            final String titleText = item.getName();
            final String descText = item.getDescription() == null ? "" : item.getDescription();
            final String miscText = "Owner: " + (item.getOwner() == null ? "Unknown" : item.getOwner().getUsername())
                    + " | Start: " + String.format("%.2f", auction.getStartPrice())
                    + " | Step: " + String.format("%.2f", auction.getBidStep());
            final String priceText = "CURRENT PRICE: " + currentPrice;
            final String endDateText = endsText.isBlank() ? "End: N/A" : "Ends at: " + endsText;

            Platform.runLater(() -> {
                setTitle(titleText);
                if (descLabel != null) descLabel.setText("Description: " + descText);
                if (miscLabel != null) miscLabel.setText(miscText);
                if (currentPriceLabel != null) currentPriceLabel.setText(priceText);
                if (endDateLabel != null) endDateLabel.setText(endDateText);
            });

        } catch (Exception e) {
            /*
             * REVIEW:
             * Không nên chỉ fallback title rồi nuốt lỗi.
             * Nếu không load được chi tiết auction, nên hiển thị notificationText hoặc Alert.
             */
            Platform.runLater(() -> setTitle(fallbackTitle));
        }

        /*
         * REVIEW - REALTIME/NETWORK MESSAGE KHÔNG KHỚP PROTOCOL:
         *
         * Đoạn này giả định server sẽ gửi AuctionResponse với ActionType.NOTIFY_NEW_BID.
         *
         * Nhưng protocol hiện tại của project cần thống nhất theo Response<T> và ActionType đã định nghĩa.
         * Nếu không có AuctionResponse, NOTIFY_NEW_BID, SubscribeAuctionRequest trong protocol chính thức
         * thì đoạn này sẽ không compile hoặc không bao giờ được server xử lý.
         *
         * Nếu muốn realtime update:
         *
         * Option 1 - đơn giản cho demo:
         * - Bỏ subscribe.
         * - Thêm nút Refresh hoặc tự gọi getAuctionDetail sau khi bid.
         *
         * Option 2 - realtime đúng protocol:
         * - Thêm ActionType.SUBSCRIBE_AUCTION
         * - Thêm ActionType.UNSUBSCRIBE_AUCTION
         * - Thêm ActionType.AUCTION_UPDATED hoặc BID_PLACED_EVENT
         * - Payload phải là DTO, ví dụ AuctionDetailDTO hoặc BidDTO, không phải server model Auction.
         * - ClientHandler/Server phải quản lý danh sách client subscribe theo auctionId.
         */
        try {
            Client client = Client.getInstance();
            if (client.isConnected()) {
                client.setOnMessageReceived(response -> {
                    /*
                     * REVIEW:
                     * Một Client chỉ có một onMessageReceived handler như thế này rất dễ bị controller khác ghi đè.
                     * Khi chuyển màn hình, controller mới set handler sẽ làm handler cũ mất.
                     *
                     * Nên có central dispatcher phía client:
                     *
                     * ClientMessageDispatcher
                     *   -> dispatch theo ActionType
                     *   -> controller/service đăng ký listener theo event
                     *
                     * Với demo hiện tại, nên tránh realtime trước nếu chưa có dispatcher.
                     */
                    if (!(response instanceof AuctionResponse)) return;

                    AuctionResponse ar = (AuctionResponse) response;
                    if (ar.getResponseType() != ActionType.NOTIFY_NEW_BID) return;

                    Auction updated = ar.getAuction();
                    if (updated == null) return;
                    if (updated.getId() != this.auctionId) return;

                    Platform.runLater(() -> {
                        currentPriceLabel.setText("CURRENT PRICE: " + String.format("%.2f", updated.getCurrentPrice()));
                        setTitle(updated.getItem() == null ? ("AUCTION #" + updated.getId()) : updated.getItem().getName());
                    });
                });

                client.sendMessage(new SubscribeAuctionRequest(auctionId, true));
            }
        } catch (Exception ignore) {
            /*
             * REVIEW:
             * Không nên nuốt lỗi subscribe.
             * Nhưng tốt hơn là chưa làm subscribe nếu protocol chưa hỗ trợ.
             */
        }
    }

    public void onBidButtonPressed() {
        try {
            /*
             * REVIEW:
             * SessionManager phía client chỉ nên dùng để kiểm tra UI/hiển thị.
             * Server vẫn phải lấy currentUser từ session server-side.
             *
             * Client không nên gửi nguyên User object lên server để server tin.
             */
            User currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                notificationText.setText("Please login before bidding!");
                return;
            }

            String bidText = bidTextField.getText().trim();
            if (bidText.isEmpty()) {
                notificationText.setText("Please enter a bid amount!");
                return;
            }

            double tryBidValue = Double.parseDouble(bidText);

            /*
             * REVIEW - SAI KIẾN TRÚC:
             *
             * Không gọi auctionService.getAuctionById(auctionId) trực tiếp từ client.
             * Nếu cần validate trước ở UI, hãy gọi AuctionClientService.getAuctionDetail(auctionId).
             *
             * Nhưng validation quyết định cuối cùng vẫn phải nằm ở server.
             */
            Auction auction = auctionService.getAuctionById(auctionId);

            if (auction == null) {
                notificationText.setText("Auction not found!");
                return;
            }

            /*
             * REVIEW:
             * Validate ở client chỉ là hỗ trợ UX, không thay thế server validation.
             * Server vẫn phải check:
             * - auction còn RUNNING
             * - amount >= currentPrice + bidStep
             * - user không phải owner
             * - user đủ balance
             * - transaction/concurrency
             */
            double currentPrice = auction.getCurrentPrice();
            double bidStep = auction.getBidStep();
            double minimumBid = currentPrice + bidStep;

            if (tryBidValue <= currentPrice) {
                notificationText.setText("Bid must be larger than current price (" + currentPrice + ")!");
                return;
            }

            if (tryBidValue < minimumBid) {
                notificationText.setText("Bid must be more than " + minimumBid + " (current + step)!");
                return;
            }

            /*
             * REVIEW - SAI KIẾN TRÚC + SAI SECURITY:
             *
             * Không được gọi service server trực tiếp:
             *
             * auctionService.placeBid(auctionId, currentUser, tryBidValue);
             *
             * Không nên truyền currentUser từ client vào backend.
             * Server phải lấy user hiện tại từ ClientHandler.currentUser.
             *
             * Cách đúng:
             *
             * bidClientService.placeBid(auctionId, tryBidValue);
             *
             * Trong đó client gửi:
             *
             * Request<PlaceBidRequest>(
             *     ActionType.PLACE_BID,
             *     new PlaceBidRequest(auctionId, tryBidValue)
             * )
             *
             * Server dùng currentUser trong ClientHandler để xác định bidder.
             */
            auctionService.placeBid(auctionId, currentUser, tryBidValue);

            Platform.runLater(() -> {
                notificationText.setText("✓ Bid placed successfully!");
                bidTextField.clear();

                /*
                 * REVIEW:
                 * Sau khi bid thành công, không nên chỉ set current price = tryBidValue một cách mù quáng.
                 * Server nên trả AuctionDetailDTO/AuctionSummaryDTO mới nhất sau khi transaction commit.
                 * Client cập nhật UI theo dữ liệu server trả về.
                 */
                currentPriceLabel.setText("CURRENT PRICE: " + String.format("%.2f", tryBidValue));
            });

        } catch (com.auction.exception.AuctionClosedException e) {
            /*
             * REVIEW:
             * Client không nên import exception phía server/domain nếu exception đó không nằm trong shared.exception.
             * Với protocol Request/Response, server nên trả Response.error(message),
             * client hiển thị errorMessage.
             */
            notificationText.setText("Auction is closed!");

        } catch (com.auction.exception.InvalidBidException e) {
            notificationText.setText("Invalid bid: " + e.getMessage());

        } catch (com.auction.exception.InsufficientFundsException e) {
            notificationText.setText("Insufficient funds in your account!");

        } catch (NumberFormatException e) {
            notificationText.setText("Please enter a valid number!");

        } catch (Exception e) {
            /*
             * REVIEW:
             * Không nên hiển thị lỗi nội bộ thô cho user nếu e.getMessage() đến từ server/repository.
             * Client nên hiển thị message đã được server đóng gói trong Response.error.
             */
            notificationText.setText("Error placing bid: " + e.getMessage());
        }
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setTitle(String text) {
        if (titleLabel != null) titleLabel.setText(text);
    }
}