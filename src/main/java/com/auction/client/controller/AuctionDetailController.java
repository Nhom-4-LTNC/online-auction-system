package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.client.session.ClientSession;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.BidClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.dto.VehicleDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.Role;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.protocol.auction.GetAuctionResponse;
import com.auction.shared.protocol.auction.UpdateAuctionRequest;
import com.auction.shared.protocol.bid.PlaceBidResponse;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
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
    @FXML private Label startTimeLabel;
    @FXML private Label winnerLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Button updateAuctionButton;
    @FXML private Button cancelAuctionButton;
    @FXML private Label messageLabel;
    @FXML private Button refreshBidHistoryButton;
    @FXML private TableView<BidDTO> bidHistoryTable;
    @FXML private TableColumn<BidDTO, String> bidderColumn;
    @FXML private TableColumn<BidDTO, String> amountColumn;
    @FXML private TableColumn<BidDTO, String> timeColumn;
    @FXML private LineChart lineChart;
    @FXML private CategoryAxis categoryAxis;

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
    private AuctionDetailDTO currentDetail;
    private Timeline countdownTimeline;
    private AuctionStatus countdownStatus;
    private long countdownStartTimeMillis;
    private long countdownEndTimeMillis;
    private Runnable onBack;

    @FXML
    private void initialize() {
        setupBidHistoryTable();
        setupButtonActions();
        updateAuctionButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        messageLabel.setText("");
        startCountdownTimeline();
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

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
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
            if (onBack != null) {
                onBack.run();
                return;
            }
            try {
                Stage stage = (Stage) root.getScene().getWindow();
                SceneUtils.switchScene(stage, "/fxml/AuctionMenu.fxml");
                stage.setMaximized(true);
            } catch (IOException e) {
                showError("Không thể quay lại danh sách đấu giá.");
            }
        });
        placeBidButton.setOnAction(event -> handlePlaceBid());
        updateAuctionButton.setOnAction(event -> handleUpdateAuction());
        cancelAuctionButton.setOnAction(event -> handleCancelAuction());
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
        startTimeLabel.setText(formatTime(summary.getStartTimeMillis()));
        endTimeLabel.setText(formatTime(summary.getEndTimeMillis()));
        winnerLabel.setText(formatWinner(summary.getStatus(), summary.getWinnerUsername()));
        updateCountdownState(summary.getStatus(), summary.getStartTimeMillis(), summary.getEndTimeMillis());

        boolean bidOpen = isAuctionBiddable(summary.getStatus());
        auctionBiddable = bidOpen;
        placeBidButton.setDisable(!bidOpen || placingBid);
        bidAmountField.setDisable(!bidOpen);
        updateAuctionButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
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

        currentDetail = detail;
        ItemDTO item = detail.getItem();
        String itemName = item == null ? "N/A" : safeText(item.getName());
        auctionTitleLabel.setText(itemName);
        itemNameLabel.setText(itemName);
        descriptionArea.setText(item == null ? "" : safeText(item.getDescription()));
        currentPriceLabel.setText(FormatUtils.currency(detail.getCurrentPrice()));
        startingPriceLabel.setText(FormatUtils.currency(detail.getStartingPrice()));
        bidStepLabel.setText(FormatUtils.currency(detail.getBidStep()));
        sellerLabel.setText(safeText(detail.getSellerUsername()));
        startTimeLabel.setText(formatTime(detail.getStartTimeMillis()));
        endTimeLabel.setText(formatTime(detail.getEndTimeMillis()));
        winnerLabel.setText(formatWinner(detail.getStatus(), detail.getWinnerUsername()));
        statusLabel.setText(formatStatus(detail.getStatus()));
        updateCountdownState(detail.getStatus(), detail.getStartTimeMillis(), detail.getEndTimeMillis());
        renderItemImage(item);

        boolean bidOpen = isAuctionBiddable(detail.getStatus());
        auctionBiddable = bidOpen;
        placeBidButton.setDisable(!bidOpen || placingBid);
        bidAmountField.setDisable(!bidOpen);
        updateAuctionButton.setDisable(!isAuctionUpdatableByCurrentUser(detail));
        cancelAuctionButton.setDisable(!isAuctionCancelableByCurrentUser(detail));
    }

    private void renderBidHistory(List<BidDTO> bids) {
        if (bids == null) {
            bidHistoryTable.getItems().clear();
            populateBidHistoryChart(null);
            return;
        }
        bidHistoryTable.setItems(FXCollections.observableArrayList(bids));
        populateBidHistoryChart(bids);
    }

    private void populateBidHistoryChart(List<BidDTO> bids) {
        if (lineChart == null) {
            return;
        }

        lineChart.getData().clear();

        if (bids == null || bids.isEmpty()) {
            return;
        }

        // Create a series for the bid amounts
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu giá");

        // Sort bids by time for proper display
        List<BidDTO> sortedBids = new java.util.ArrayList<>(bids);
        sortedBids.sort((a, b) -> Long.compare(a.getBidTime(), b.getBidTime()));

        // Add data points to the series
        for (int i = 0; i < sortedBids.size(); i++) {
            BidDTO bid = sortedBids.get(i);
            String label = "Bid " + (i + 1);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, bid.getAmount());
            series.getData().add(dataPoint);
        }

        lineChart.getData().add(series);
    }

    private void addLatestBidToHistory(BidDTO latestBid) {
        if (latestBid == null || latestBid.getAuctionId() != currentAuctionId) {
            return;
        }

        if (bidHistoryTable.getItems() == null) {
            bidHistoryTable.setItems(FXCollections.observableArrayList());
        }

        boolean alreadyExists = bidHistoryTable.getItems().stream()
                .anyMatch(existing -> isSameBid(existing, latestBid));
        if (alreadyExists) {
            return;
        }

        bidHistoryTable.getItems().add(0, latestBid);
        bidHistoryTable.getItems().sort((left, right) ->
                Long.compare(right.getBidTime(), left.getBidTime()));

        // Update the chart with the new bid
        populateBidHistoryChart(new java.util.ArrayList<>(bidHistoryTable.getItems()));
    }

    private boolean isSameBid(BidDTO existing, BidDTO latestBid) {
        if (existing == null || latestBid == null) {
            return false;
        }
        if (existing.getBidId() > 0 && latestBid.getBidId() > 0) {
            return existing.getBidId() == latestBid.getBidId();
        }
        return existing.getAuctionId() == latestBid.getAuctionId()
                && existing.getBidderId() == latestBid.getBidderId()
                && Double.compare(existing.getAmount(), latestBid.getAmount()) == 0
                && existing.getBidTime() == latestBid.getBidTime();
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

    private void handleUpdateAuction() {
        if (currentDetail == null || currentDetail.getItem() == null) {
            showError("Khong co thong tin phien dau gia de cap nhat.");
            return;
        }

        ItemDTO item = currentDetail.getItem();
        Dialog<UpdateAuctionRequest> dialog = new Dialog<>();
        dialog.setTitle("Sua phien dau gia");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(safeText(item.getName()));
        TextArea descriptionField = new TextArea(safeText(item.getDescription()));
        descriptionField.setPrefRowCount(3);
        TextField startTimeField = new TextField(formatTime(currentDetail.getStartTimeMillis()));
        TextField endTimeField = new TextField(formatTime(currentDetail.getEndTimeMillis()));
        TextField subtypeField1 = new TextField();
        TextField subtypeField2 = new TextField();
        String subtypeLabel1 = "Field 1";
        String subtypeLabel2 = "Field 2";

        if (item instanceof ArtDTO art) {
            subtypeLabel1 = "Artist";
            subtypeLabel2 = "Year";
            subtypeField1.setText(safeText(art.getArtist()));
            subtypeField2.setText(String.valueOf(art.getYearCreated()));
        } else if (item instanceof ElectronicsDTO electronics) {
            subtypeLabel1 = "Brand";
            subtypeLabel2 = "Warranty months";
            subtypeField1.setText(safeText(electronics.getBrand()));
            subtypeField2.setText(String.valueOf(electronics.getWarrantyMonths()));
        } else if (item instanceof VehicleDTO vehicle) {
            subtypeLabel1 = "Brand";
            subtypeLabel2 = "VIN / Mileage";
            subtypeField1.setText(safeText(vehicle.getBrand()));
            subtypeField2.setText(safeText(vehicle.getVin()) + " / " + vehicle.getMileage());
        }

        File[] selectedImage = new File[1];
        Label imageLabel = new Label("Giu anh hien tai");
        Button imageButton = new Button("Chon anh moi");
        imageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                selectedImage[0] = file;
                imageLabel.setText(file.getName());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Ten"), nameField);
        form.addRow(1, new Label("Mo ta"), descriptionField);
        form.addRow(2, new Label("Bat dau"), startTimeField);
        form.addRow(3, new Label("Ket thuc"), endTimeField);
        form.addRow(4, new Label(subtypeLabel1), subtypeField1);
        form.addRow(5, new Label(subtypeLabel2), subtypeField2);
        form.addRow(6, imageButton, imageLabel);
        dialog.getDialogPane().setContent(new VBox(8, form));

        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            try {
                byte[] imageData = selectedImage[0] == null ? null : Files.readAllBytes(selectedImage[0].toPath());
                String imageFileName = selectedImage[0] == null ? null : selectedImage[0].getName();
                ItemDTO updatedItem = buildUpdatedItemDTO(
                        item,
                        nameField.getText(),
                        descriptionField.getText(),
                        imageData,
                        imageFileName,
                        subtypeField1.getText(),
                        subtypeField2.getText()
                );
                return new UpdateAuctionRequest(
                        currentAuctionId,
                        updatedItem,
                        parseUpdateTime(startTimeField.getText()),
                        parseUpdateTime(endTimeField.getText())
                );
            } catch (Exception e) {
                showError(errorMessage(e));
                return null;
            }
        });

        dialog.showAndWait().ifPresent(this::submitUpdateAuction);
    }

    private ItemDTO buildUpdatedItemDTO(ItemDTO existingItem,
                                        String name,
                                        String description,
                                        byte[] imageData,
                                        String imageFileName,
                                        String subtypeValue1,
                                        String subtypeValue2) {
        if (existingItem instanceof ArtDTO) {
            return new ArtDTO(name, description, existingItem.getStartingPrice(),
                    imageData, imageFileName, subtypeValue1, Integer.parseInt(subtypeValue2.trim()));
        }
        if (existingItem instanceof ElectronicsDTO) {
            return new ElectronicsDTO(name, description, existingItem.getStartingPrice(),
                    imageData, imageFileName, subtypeValue1, Integer.parseInt(subtypeValue2.trim()));
        }
        if (existingItem instanceof VehicleDTO vehicle) {
            String vin = vehicle.getVin();
            int mileage = vehicle.getMileage();
            String raw = subtypeValue2 == null ? "" : subtypeValue2.trim();
            int separator = raw.indexOf('/');
            if (separator >= 0) {
                vin = raw.substring(0, separator).trim();
                mileage = Integer.parseInt(raw.substring(separator + 1).trim());
            }
            return new VehicleDTO(name, description, existingItem.getStartingPrice(),
                    imageData, imageFileName, subtypeValue1, vin, mileage);
        }
        throw new IllegalArgumentException("Loai san pham khong duoc ho tro cap nhat.");
    }

    private long parseUpdateTime(String text) {
        LocalDateTime value = LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void submitUpdateAuction(UpdateAuctionRequest request) {
        updateAuctionButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        showInfo("Dang cap nhat phien dau gia...");

        Task<CreateAuctionResponse> task = new Task<>() {
            @Override
            protected CreateAuctionResponse call() {
                return auctionClientService.updateAuctionItem(request);
            }
        };

        task.setOnSucceeded(event -> {
            CreateAuctionResponse response = task.getValue();
            showInfo(response == null ? "Cap nhat phien dau gia thanh cong." : response.getMessage());
            loadAuctionDetailPageAsync(false);
        });
        task.setOnFailed(event -> {
            updateAuctionButton.setDisable(false);
            cancelAuctionButton.setDisable(false);
            showError(errorMessage(task.getException()));
        });
        task.setOnCancelled(event -> {
            updateAuctionButton.setDisable(false);
            cancelAuctionButton.setDisable(false);
        });

        runDaemon(task, "auction-detail-update-auction");
    }

    private void handleCancelAuction() {
        if (currentAuctionId <= 0) {
            return;
        }

        cancelAuctionButton.setDisable(true);
        refreshBidHistoryButton.setDisable(true);
        showInfo("Dang huy phien dau gia...");

        int auctionIdSnapshot = currentAuctionId;
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return auctionClientService.cancelAuction(auctionIdSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            showInfo(task.getValue() == null ? "Huy phien dau gia thanh cong." : task.getValue());
            loadAuctionDetailPageAsync(false);
        });
        task.setOnFailed(event -> {
            cancelAuctionButton.setDisable(false);
            refreshBidHistoryButton.setDisable(loadingBidHistory);
            showError(errorMessage(task.getException()));
        });
        task.setOnCancelled(event -> {
            cancelAuctionButton.setDisable(false);
            refreshBidHistoryButton.setDisable(loadingBidHistory);
        });

        runDaemon(task, "auction-detail-cancel-auction");
    }

    /**
     * Registers the AUCTION_UPDATED listener for this detail screen.
     *
     * <p>The listener filters by auctionId before applying changes. BID_PLACED
     * can update bid history with latestBid; other update types reload detail
     * data from the server source of truth.</p>
     */
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
                    BidDTO latestBid = event.getLatestBid();
                    if (latestBid != null) {
                        addLatestBidToHistory(latestBid);
                    } else {
                        reloadBidHistoryForRealtime();
                    }
                } else {
                    reloadAuctionDetailForRealtime();
                }
            };
        }

        client.addEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
        realtimeListenerRegistered = true;
    }

    /**
     * Removes the realtime listener to avoid duplicated updates after reopening detail.
     */
    private void unregisterRealtimeListener() {
        if (realtimeListenerRegistered && auctionUpdatedListener != null) {
            client.removeEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
        }
        realtimeListenerRegistered = false;
    }

    /**
     * Releases JavaFX lifecycle resources for this screen.
     *
     * <p>Call when navigating away or closing the modal. It prevents memory
     * leaks by removing the realtime listener and stopping the countdown
     * Timeline.</p>
     */
    public void cleanup() {
        unregisterRealtimeListener();
        stopCountdownTimeline();
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
        if (epochMillis <= 0) {
            return "--";
        }
        try {
            return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
        } catch (Exception e) {
            return String.valueOf(epochMillis);
        }
    }

    private void startCountdownTimeline() {
        stopCountdownTimeline();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdownLabel()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdownLabel();
    }

    private void stopCountdownTimeline() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void updateCountdownState(AuctionStatus status, long startTimeMillis, long endTimeMillis) {
        countdownStatus = status;
        countdownStartTimeMillis = startTimeMillis;
        countdownEndTimeMillis = endTimeMillis;
        updateCountdownLabel();
    }

    private void updateCountdownLabel() {
        if (timeRemainingLabel == null) {
            return;
        }

        if (countdownStatus == AuctionStatus.OPEN) {
            timeRemainingLabel.setText("Bắt đầu sau: " + formatDuration(countdownStartTimeMillis - System.currentTimeMillis()));
            return;
        }
        if (countdownStatus == AuctionStatus.RUNNING) {
            timeRemainingLabel.setText("Còn lại: " + formatDuration(countdownEndTimeMillis - System.currentTimeMillis()));
            return;
        }
        if (countdownStatus == AuctionStatus.FINISHED) {
            timeRemainingLabel.setText("Đã kết thúc");
            return;
        }
        if (countdownStatus == AuctionStatus.PAID) {
            timeRemainingLabel.setText("Đã thanh toán");
            return;
        }
        if (countdownStatus == AuctionStatus.CANCELED) {
            timeRemainingLabel.setText("Đã hủy");
            return;
        }
        timeRemainingLabel.setText("--");
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(millis));
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String formatWinner(AuctionStatus status, String winnerUsername) {
        if (winnerUsername != null && !winnerUsername.isBlank()) {
            return winnerUsername;
        }
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            return "Không có người thắng";
        }
        return "Chưa xác định";
    }

    private String formatStatus(AuctionStatus status) {
        return "Trạng thái: " + switch (status) {
            case OPEN -> "Sắp diễn ra";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
            case null -> "N/A";
        };
    }

    private boolean isAuctionBiddable(AuctionStatus status) {
        return status == AuctionStatus.RUNNING;
    }

    private boolean isAuctionCancelableByCurrentUser(AuctionDetailDTO detail) {
        if (detail == null || detail.getStatus() == AuctionStatus.CANCELED) {
            return false;
        }

        UserDTO user = ClientSession.getCurrentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getId() == detail.getSellerId() && detail.getStatus() == AuctionStatus.OPEN;
    }

    private boolean isAuctionUpdatableByCurrentUser(AuctionDetailDTO detail) {
        if (detail == null || detail.getStatus() != AuctionStatus.OPEN) {
            return false;
        }

        UserDTO user = ClientSession.getCurrentUser();
        return user != null && user.getId() == detail.getSellerId();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private record AuctionDetailPage(AuctionDetailDTO detail, List<BidDTO> bids) {}
}
