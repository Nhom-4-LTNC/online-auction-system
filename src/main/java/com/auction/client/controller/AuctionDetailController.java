package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.BidClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.GetAuctionResponse;
import com.auction.shared.protocol.bid.PlaceBidResponse;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class AuctionDetailController {

    @FXML private BorderPane root;
    @FXML private Button backButton;
    @FXML private Label auctionTitleLabel;
    @FXML private Label statusLabel;
    @FXML private ImageView itemImageView;
    @FXML private Label itemNameLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label currentPriceLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label bidStepLabel;
    @FXML private Label sellerLabel;
    @FXML private Label endTimeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Button closeAuctionButton;
    @FXML private Label messageLabel;
    @FXML private Button refreshBidHistoryButton;
    @FXML private TableView<BidDTO> bidHistoryTable;
    @FXML private TableColumn<BidDTO, String> bidderColumn;
    @FXML private TableColumn<BidDTO, String> amountColumn;
    @FXML private TableColumn<BidDTO, String> timeColumn;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final BidClientService bidClientService = new BidClientService();
    private final Client client = Client.getInstance();

    private int currentAuctionId;
    private Consumer<Response<?>> auctionUpdatedListener;
    private boolean realtimeListenerRegistered = false;
    private volatile boolean pageLoading = false;
    private volatile boolean bidHistoryReloading = false;
    private volatile boolean loadingBidHistory = false;
    private volatile boolean placingBid = false;
    private volatile boolean auctionBiddable = false;

    @FXML
    private void initialize() {
        setupBidHistoryTable();
        setupButtonActions();
        closeAuctionButton.setDisable(true);
        messageLabel.setText("");
        itemImageView.setSmooth(true);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                cleanup();
            }
            if (newScene != null) {
                Platform.runLater(this::maximizeStage);
            }
        });
    }

    public void setAuctionId(int auctionId) {
        currentAuctionId = auctionId;
        showLoadingState();
        loadAuctionDetailPageAsync(false);
        registerRealtimeListener();
    }

    public void setInitialAuction(AuctionSummaryDTO summary) {
        if (summary == null) {
            return;
        }

        currentAuctionId = summary.getAuctionId();
        renderSummaryImmediately(summary);
        showLoadingState();
        loadAuctionDetailPageAsync(false);
        registerRealtimeListener();
    }

    private void setupBidHistoryTable() {
        bidderColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getBidderUsername())));
        amountColumn.setCellValueFactory(data ->
                new SimpleStringProperty(FormatUtils.currency(data.getValue().getAmount())));
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().getBidTime())));
    }

    private void setupButtonActions() {
        backButton.setOnAction(event -> {
            cleanup();
            try {
                Stage stage = (Stage) root.getScene().getWindow();
                SceneUtils.switchScene(stage, "/fxml/AuctionMenu.fxml");
                stage.setMaximized(true);
            } catch (IOException e) {
                showError("Không thể quay lại danh sách đấu giá.");
            }
        });
        placeBidButton.setOnAction(event -> handlePlaceBid());
        closeAuctionButton.setOnAction(event -> handleCloseAuction());
        refreshBidHistoryButton.setOnAction(event -> loadBidHistoryAsync());
    }

    private void loadAuctionDetailPageAsync(boolean showRealtimeMessage) {
        if (pageLoading) {
            return;
        }
        pageLoading = true;
        refreshBidHistoryButton.setDisable(true);

        int auctionIdSnapshot = currentAuctionId;
        Task<AuctionDetailPage> task = new Task<>() {
            @Override
            protected AuctionDetailPage call() {
                GetAuctionResponse response = auctionClientService.getAuctionResponse(auctionIdSnapshot);
                AuctionDetailDTO detail = response == null ? null : response.getAuction();
                List<BidDTO> bids = response == null ? null : response.getRecentBids();
                if (bids == null) {
                    bids = bidClientService.getBidHistoryByAuction(auctionIdSnapshot);
                }
                return new AuctionDetailPage(detail, bids);
            }
        };

        task.setOnSucceeded(event -> {
            pageLoading = false;
            refreshBidHistoryButton.setDisable(loadingBidHistory);
            if (auctionIdSnapshot == currentAuctionId) {
                AuctionDetailPage page = task.getValue();
                renderAuctionDetail(page.detail());
                renderBidHistory(page.bids());
                if (showRealtimeMessage) {
                    showInfo("Phiên đấu giá đã được cập nhật.");
                }
            }
        });
        task.setOnFailed(event -> {
            pageLoading = false;
            refreshBidHistoryButton.setDisable(loadingBidHistory);
            showError("Không thể tải chi tiết phiên đấu giá: " + errorMessage(task.getException()));
        });
        task.setOnCancelled(event -> {
            pageLoading = false;
            refreshBidHistoryButton.setDisable(loadingBidHistory);
        });

        runDaemon(task, "auction-detail-page-loader");
    }

    private void loadBidHistoryAsync() {
        if (loadingBidHistory) {
            return;
        }
        loadingBidHistory = true;
        refreshBidHistoryButton.setDisable(true);
        showInfo("Đang tải lịch sử bid...");
        int auctionIdSnapshot = currentAuctionId;
        Task<List<BidDTO>> task = new Task<>() {
            @Override
            protected List<BidDTO> call() {
                return bidClientService.getBidHistoryByAuction(auctionIdSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            loadingBidHistory = false;
            refreshBidHistoryButton.setDisable(false);
            if (auctionIdSnapshot == currentAuctionId) {
                renderBidHistory(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            loadingBidHistory = false;
            refreshBidHistoryButton.setDisable(false);
            showError("Không thể tải lịch sử bid: " + errorMessage(task.getException()));
        });
        task.setOnCancelled(event -> {
            loadingBidHistory = false;
            refreshBidHistoryButton.setDisable(false);
        });

        runDaemon(task, "bid-history-loader");
    }

    private void reloadAuctionDetailForRealtime() {
        loadAuctionDetailPageAsync(true);
    }

    private void reloadBidHistoryForRealtime() {
        if (bidHistoryReloading) {
            return;
        }
        bidHistoryReloading = true;

        int auctionIdSnapshot = currentAuctionId;
        Thread thread = new Thread(() -> {
            try {
                List<BidDTO> bids = bidClientService.getBidHistoryByAuction(auctionIdSnapshot);
                javafx.application.Platform.runLater(() -> {
                    if (auctionIdSnapshot == currentAuctionId) {
                        renderBidHistory(bids);
                    }
                });
            } catch (Exception e) {
                System.err.println("[AuctionDetailController] Failed to reload bid history after realtime update: "
                        + e.getMessage());
            } finally {
                bidHistoryReloading = false;
            }
        }, "bid-history-realtime-loader");

        thread.setDaemon(true);
        thread.start();
    }

    private void applyRealtimeSummary(AuctionUpdatedEvent event) {
        if (event == null || event.getSummary() == null) {
            return;
        }

        renderSummaryImmediately(event.getSummary());
    }

    private void renderSummaryImmediately(AuctionSummaryDTO summary) {
        if (summary == null) {
            return;
        }

        String itemName = safeText(summary.getItemName());
        auctionTitleLabel.setText(itemName);
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText(FormatUtils.currency(summary.getCurrentPrice()));
        statusLabel.setText(formatStatus(summary.getStatus()));
        endTimeLabel.setText(formatTime(summary.getEndTimeMillis()));

        boolean bidOpen = isAuctionBiddable(summary.getStatus());
        auctionBiddable = bidOpen;
        placeBidButton.setDisable(!bidOpen || placingBid);
        bidAmountField.setDisable(!bidOpen);
    }

    private void showLoadingState() {
        descriptionArea.setText("Đang tải mô tả...");
        sellerLabel.setText("Đang tải...");
        bidStepLabel.setText("Đang tải...");
        startingPriceLabel.setText("Đang tải...");
        bidHistoryTable.setPlaceholder(new Label("Đang tải lịch sử trả giá..."));
        refreshBidHistoryButton.setDisable(true);
    }

    private void renderAuctionDetail(AuctionDetailDTO detail) {
        if (detail == null) {
            showError("Không tìm thấy chi tiết phiên đấu giá.");
            return;
        }

        ItemDTO item = detail.getItem();
        String itemName = item == null ? "N/A" : safeText(item.getName());
        auctionTitleLabel.setText(itemName);
        itemNameLabel.setText(itemName);
        descriptionArea.setText(item == null ? "" : safeText(item.getDescription()));
        currentPriceLabel.setText(FormatUtils.currency(detail.getCurrentPrice()));
        startingPriceLabel.setText(FormatUtils.currency(detail.getStartingPrice()));
        bidStepLabel.setText(FormatUtils.currency(detail.getBidStep()));
        sellerLabel.setText(safeText(detail.getSellerUsername()));
        endTimeLabel.setText(formatTime(detail.getEndTimeMillis()));
        statusLabel.setText(formatStatus(detail.getStatus()));
        renderItemImage(item);

        boolean bidOpen = isAuctionBiddable(detail.getStatus());
        auctionBiddable = bidOpen;
        placeBidButton.setDisable(!bidOpen || placingBid);
        bidAmountField.setDisable(!bidOpen);
    }

    private void renderBidHistory(List<BidDTO> bids) {
        if (bids == null) {
            bidHistoryTable.getItems().clear();
            return;
        }
        bidHistoryTable.setItems(FXCollections.observableArrayList(bids));
    }

    private void handlePlaceBid() {
        if (placingBid) {
            return;
        }
        BigDecimal amount;
        try {
            amount = parseBidAmount();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        placingBid = true;
        placeBidButton.setDisable(true);
        refreshBidHistoryButton.setDisable(true);
        showInfo("Đang xử lý đặt giá...");

        Task<PlaceBidResponse> task = new Task<>() {
            @Override
            protected PlaceBidResponse call() {
                return bidClientService.placeBid(currentAuctionId, amount);
            }
        };

        task.setOnSucceeded(event -> {
            placingBid = false;
            placeBidButton.setDisable(!auctionBiddable);
            refreshBidHistoryButton.setDisable(loadingBidHistory);
            bidAmountField.clear();
            PlaceBidResponse response = task.getValue();
            showInfo(response == null ? "Đặt giá thành công." : response.getMessage());
            loadAuctionDetailPageAsync(false);
        });
        task.setOnFailed(event -> {
            placingBid = false;
            placeBidButton.setDisable(!auctionBiddable);
            refreshBidHistoryButton.setDisable(loadingBidHistory);
            showError(errorMessage(task.getException()));
        });
        task.setOnCancelled(event -> {
            placingBid = false;
            placeBidButton.setDisable(!auctionBiddable);
            refreshBidHistoryButton.setDisable(loadingBidHistory);
        });

        runDaemon(task, "place-bid-submit");
    }

    private void handleCloseAuction() {
        showInfo("Màn hình này chưa hỗ trợ đóng phiên đấu giá.");
    }

    private void registerRealtimeListener() {
        if (realtimeListenerRegistered) {
            return;
        }

        if (auctionUpdatedListener == null) {
            auctionUpdatedListener = response -> {
                if (response == null || response.getAction() != ActionType.AUCTION_UPDATED) {
                    return;
                }
                Object payload = response.getPayload();
                if (!(payload instanceof AuctionUpdatedEvent event)) {
                    return;
                }
                if (event.getAuctionId() != currentAuctionId) {
                    return;
                }

                applyRealtimeSummary(event);
                if (event.getUpdateType() == AuctionUpdateType.BID_PLACED) {
                    reloadBidHistoryForRealtime();
                } else {
                    reloadAuctionDetailForRealtime();
                }
            };
        }

        client.addEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
        realtimeListenerRegistered = true;
    }

    private void unregisterRealtimeListener() {
        if (realtimeListenerRegistered && auctionUpdatedListener != null) {
            client.removeEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
        }
        realtimeListenerRegistered = false;
    }

    public void cleanup() {
        unregisterRealtimeListener();
    }

    private void maximizeStage() {
        if (root == null || root.getScene() == null || root.getScene().getWindow() == null) {
            return;
        }
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setMaximized(true);
    }

    private BigDecimal parseBidAmount() {
        String rawAmount = bidAmountField.getText();
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền đặt giá.");
        }

        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền đặt giá phải là số dương.");
        }
    }

    private void renderItemImage(ItemDTO item) {
        if (item == null || item.getImageData() == null || item.getImageData().length == 0) {
            itemImageView.setImage(null);
            return;
        }

        try {
            itemImageView.setImage(new Image(new ByteArrayInputStream(item.getImageData())));
        } catch (Exception ignored) {
            itemImageView.setImage(null);
        }
    }

    private void runDaemon(Runnable runnable, String threadName) {
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String message) {
        messageLabel.setText(safeText(message));
    }

    private void showInfo(String message) {
        messageLabel.setText(safeText(message));
    }

    private String errorMessage(Throwable error) {
        if (error instanceof ClientServiceException && error.getMessage() != null) {
            return error.getMessage();
        }
        return error == null || error.getMessage() == null ? "Lỗi không xác định." : error.getMessage();
    }

    private String formatTime(long epochMillis) {
        try {
            return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
        } catch (Exception e) {
            return String.valueOf(epochMillis);
        }
    }

    private String formatStatus(AuctionStatus status) {
        return "Trạng thái: " + (status == null ? "N/A" : status.name());
    }

    private boolean isAuctionBiddable(AuctionStatus status) {
        return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private record AuctionDetailPage(AuctionDetailDTO detail, List<BidDTO> bids) {}
}
