package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.client.session.ClientSession;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.AuthClientService;
import com.auction.client.service.BidClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.service.WalletClientService;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.BidDTO;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class AuctionMenuController {
    private enum AuctionListMode {
        ALL,
        MY_CREATED,
        MY_PARTICIPATED,
        MY_WON
    }

    private enum StatusFilter {
        ALL(null),
        OPEN(AuctionStatus.OPEN),
        RUNNING(AuctionStatus.RUNNING),
        FINISHED(AuctionStatus.FINISHED);

        private final AuctionStatus status;

        StatusFilter(AuctionStatus status) {
            this.status = status;
        }
    }

    private static final DateTimeFormatter END_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();
    private final BidClientService bidClientService = new BidClientService();
    private final WalletClientService walletClientService = new WalletClientService();
    private final Client client = Client.getInstance();

    @FXML private ToggleButton allAuctionsToggle;
    @FXML private ToggleButton openAuctionsToggle;
    @FXML private ToggleButton runningAuctionsToggle;
    @FXML private ToggleButton finishedAuctionsToggle;
    @FXML private ScrollPane auctionScrollPane;
    @FXML private TilePane auctionGrid;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button topUpButton;
    @FXML private Button allAuctionsButton;
    @FXML private Button createAuctionButton;
    @FXML private Button myBidsButton;
    @FXML private Button myParticipatedButton;
    @FXML private Button myWonButton;
    @FXML private Button myCreatedButton;
    @FXML private Label titleLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label balanceLabel;

    private UserDTO currentUser;
    private AuctionListMode currentMode = AuctionListMode.ALL;
    private StatusFilter currentStatusFilter = StatusFilter.ALL;
    private ToggleGroup statusToggleGroup;
    private Consumer<Response<?>> auctionUpdatedListener;
    private boolean realtimeListenerRegistered = false;
    private volatile boolean loadingAuctions = false;
    private volatile boolean realtimeReloading = false;

    @FXML
    public void initialize() {
        setupStatusFilter();
        setupAuctionList();
        currentUser = ClientSession.getCurrentUser();
        renderUserInfo();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);
        topUpButton.setOnAction(event -> showDepositDialog());
        setupSidebarActions();

        registerRealtimeListener();
        auctionGrid.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                cleanup();
            }
        });

        loadAuctionsForCurrentMode();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            cleanup();
            logoutServerSideAsync();
            ClientSession.clear();
            SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể quay về màn hình đăng nhập.");
        }
    }

    private void logoutServerSideAsync() {
        Thread thread = new Thread(() -> {
            try {
                authClientService.logout();
            } catch (Exception ignored) {
                // Local logout should still continue if the socket is already closed.
            }
        }, "auction-menu-logout");
        thread.setDaemon(true);
        thread.start();
    }

    public void setCurrentUser(UserDTO user) {
        currentUser = user;
        if (user != null) {
            ClientSession.setCurrentUser(user);
        }
        renderUserInfo();
        currentMode = AuctionListMode.ALL;
        selectStatusFilter(StatusFilter.ALL);
        setTitle("Danh sách đấu giá");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    private void renderUserInfo() {
        if (userInfoLabel == null && balanceLabel == null) {
            return;
        }

        if (currentUser == null) {
            if (userInfoLabel != null) {
                userInfoLabel.setText("Chào, Guest");
            }
            if (balanceLabel != null) {
                balanceLabel.setText("Số dư: N/A");
            }
            return;
        }

        if (userInfoLabel != null) {
            String username = currentUser.getUsername();
            userInfoLabel.setText("Chào, " + (username == null || username.isBlank() ? "N/A" : username));
        }
        if (balanceLabel != null) {
            Double balance = ClientSession.getBalance();
            balanceLabel.setText("Số dư: " + (balance == null ? "N/A" : FormatUtils.currency(balance)));
        }
    }

    private void showDepositDialog() {
        if (!ClientSession.isLoggedIn()) {
            AlertUtils.showError("Nạp tiền", "Vui lòng đăng nhập trước.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nạp tiền");
        dialog.initOwner(auctionGrid.getScene().getWindow());

        ButtonType confirmType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, cancelType);

        Label balanceValueLabel = new Label();
        Label availableValueLabel = new Label();
        // TODO: show active leading amount when the wallet API exposes that field.
        Label leadingValueLabel = new Label("-");
        Label unpaidValueLabel = new Label();
        TextField amountField = new TextField();
        amountField.setPromptText("Nhập số tiền muốn nạp");
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);

        GridPane walletGrid = new GridPane();
        walletGrid.setHgap(12);
        walletGrid.setVgap(8);
        walletGrid.addRow(0, new Label("Số dư hiện tại:"), balanceValueLabel);
        walletGrid.addRow(1, new Label("Số dư khả dụng:"), availableValueLabel);
        walletGrid.addRow(2, new Label("Đang giữ do dẫn giá:"), leadingValueLabel);
        walletGrid.addRow(3, new Label("Cần thanh toán:"), unpaidValueLabel);
        walletGrid.addRow(4, new Label("Số tiền nạp:"), amountField);

        VBox content = new VBox(12, walletGrid, messageLabel);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        updateDepositDialogWalletLabels(balanceValueLabel, availableValueLabel, unpaidValueLabel);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            double amount;
            try {
                amount = parsePositiveAmount(amountField.getText());
            } catch (IllegalArgumentException e) {
                messageLabel.setStyle("-fx-text-fill: #b00020;");
                messageLabel.setText(e.getMessage());
                return;
            }

            submitQuickDeposit(amount, confirmButton, amountField, messageLabel,
                    balanceValueLabel, availableValueLabel, unpaidValueLabel);
        });

        dialog.showAndWait();
    }

    private void submitQuickDeposit(
            double amount,
            Button confirmButton,
            TextField amountField,
            Label messageLabel,
            Label balanceValueLabel,
            Label availableValueLabel,
            Label unpaidValueLabel
    ) {
        confirmButton.setDisable(true);
        amountField.setDisable(true);
        messageLabel.setStyle("-fx-text-fill: #444444;");
        messageLabel.setText("Đang xử lý...");

        Task<BalanceResponse> task = new Task<>() {
            @Override
            protected BalanceResponse call() {
                return walletClientService.addBalance(amount);
            }
        };

        task.setOnSucceeded(event -> {
            confirmButton.setDisable(false);
            amountField.setDisable(false);
            amountField.clear();

            BalanceResponse response = task.getValue();
            updateSessionWallet(response);
            renderUserInfo();
            updateDepositDialogWalletLabels(balanceValueLabel, availableValueLabel, unpaidValueLabel);

            messageLabel.setStyle("-fx-text-fill: #1b5e20;");
            messageLabel.setText("Nạp tiền thành công.");
        });

        task.setOnFailed(event -> {
            confirmButton.setDisable(false);
            amountField.setDisable(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể nạp tiền.";
            messageLabel.setStyle("-fx-text-fill: #b00020;");
            messageLabel.setText(message);
        });

        task.setOnCancelled(event -> {
            confirmButton.setDisable(false);
            amountField.setDisable(false);
            messageLabel.setStyle("-fx-text-fill: #b00020;");
            messageLabel.setText("Yêu cầu nạp tiền đã bị hủy.");
        });

        Thread thread = new Thread(task, "auction-menu-quick-deposit");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateDepositDialogWalletLabels(
            Label balanceValueLabel,
            Label availableValueLabel,
            Label unpaidValueLabel
    ) {
        setMoneyLabel(balanceValueLabel, ClientSession.getBalance());
        setMoneyLabel(availableValueLabel, ClientSession.getAvailableBalance());
        setMoneyLabel(unpaidValueLabel, ClientSession.getUnpaidWinningAmount());
    }

    private void updateSessionWallet(BalanceResponse response) {
        if (response == null) {
            return;
        }

        ClientSession.updateWalletSummary(
                response.getBalance(),
                response.getUnpaidWinningAmount(),
                response.getAvailableBalance()
        );
    }

    private void updateSessionWallet(PayAuctionResponse response) {
        if (response == null) {
            return;
        }

        ClientSession.updateWalletSummary(
                response.getNewBalance(),
                response.getNewUnpaidWinningAmount(),
                response.getNewAvailableBalance()
        );
    }

    private void setMoneyLabel(Label label, Double value) {
        label.setText(value == null ? "-" : FormatUtils.currency(value));
    }

    private double parsePositiveAmount(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền.");
        }

        try {
            double amount = Double.parseDouble(rawValue.trim());
            if (amount <= 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền phải là số hợp lệ và lớn hơn 0.");
        }
    }

    private void setupStatusFilter() {
        statusToggleGroup = new ToggleGroup();
        allAuctionsToggle.setToggleGroup(statusToggleGroup);
        openAuctionsToggle.setToggleGroup(statusToggleGroup);
        runningAuctionsToggle.setToggleGroup(statusToggleGroup);
        finishedAuctionsToggle.setToggleGroup(statusToggleGroup);

        allAuctionsToggle.setUserData(StatusFilter.ALL);
        openAuctionsToggle.setUserData(StatusFilter.OPEN);
        runningAuctionsToggle.setUserData(StatusFilter.RUNNING);
        finishedAuctionsToggle.setUserData(StatusFilter.FINISHED);
        allAuctionsToggle.setSelected(true);
        styleStatusToggles();

        statusToggleGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                if (oldValue != null) {
                    oldValue.setSelected(true);
                }
                return;
            }
            if (!(newValue.getUserData() instanceof StatusFilter selectedFilter)) {
                return;
            }

            currentStatusFilter = selectedFilter;
            styleStatusToggles();
            loadAuctionsForCurrentMode();
        });
    }

    private void setupSidebarActions() {
        allAuctionsButton.setOnAction(event -> showAllAuctions());
        myParticipatedButton.setOnAction(event -> showMyParticipatedAuctions());
        myWonButton.setOnAction(event -> showMyWonAuctions());
        myCreatedButton.setOnAction(event -> showMyCreatedAuctions());
        createAuctionButton.setOnAction(event -> showCreateAuctionDialog());
        myBidsButton.setOnAction(event -> handleShowMyBids());
        updateSidebarSelection();
    }

    private void setupAuctionList() {
        auctionGrid.setPrefTileWidth(260);
        auctionGrid.setPrefTileHeight(300);
    }

    @FXML
    private void handleShowMyBids() {
        if (auctionGrid == null || auctionGrid.getScene() == null) {
            return;
        }

        Stage owner = (Stage) auctionGrid.getScene().getWindow();
        Stage dialog = new Stage();
        dialog.setTitle("Lịch sử đặt giá của tôi");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setResizable(true);

        Label title = new Label("Lịch sử đặt giá của tôi");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0B5394;");

        Label messageLabel = new Label("Đang tải...");
        messageLabel.setStyle("-fx-text-fill: #555555;");
        messageLabel.setWrapText(true);

        TableView<BidDTO> bidTable = createMyBidsTable();
        bidTable.setPlaceholder(new Label("Đang tải..."));

        Button closeButton = new Button("Đóng");
        closeButton.setOnAction(event -> dialog.close());

        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, title, messageLabel, bidTable, footer);
        content.setPadding(new Insets(16));
        VBox.setVgrow(bidTable, Priority.ALWAYS);

        dialog.setScene(new Scene(content, 820, 500));
        loadMyBidsAsync(bidTable, messageLabel);
        dialog.show();
    }

    private TableView<BidDTO> createMyBidsTable() {
        TableView<BidDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BidDTO, String> auctionIdColumn = new TableColumn<>("Mã phiên");
        auctionIdColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getAuctionId())));

        TableColumn<BidDTO, String> bidderColumn = new TableColumn<>("Người đặt");
        bidderColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(safeText(cell.getValue().getBidderUsername())));

        TableColumn<BidDTO, String> amountColumn = new TableColumn<>("Số tiền");
        amountColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(FormatUtils.currency(cell.getValue().getAmount())));

        TableColumn<BidDTO, String> bidTimeColumn = new TableColumn<>("Thời gian");
        bidTimeColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatBidTime(cell.getValue().getBidTime())));

        TableColumn<BidDTO, Void> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button detailButton = new Button("Xem phiên");

            {
                detailButton.setStyle("-fx-background-color: #0B5394; -fx-text-fill: white; -fx-font-weight: bold;");
                detailButton.setOnAction(event -> {
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        return;
                    }
                    BidDTO bid = getTableView().getItems().get(getIndex());
                    Stage dialog = (Stage) detailButton.getScene().getWindow();
                    dialog.close();
                    openAuctionDetailById(bid.getAuctionId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(detailButton);
            }
        });

        table.getColumns().addAll(auctionIdColumn, bidderColumn, amountColumn, bidTimeColumn, actionColumn);
        return table;
    }

    private void loadMyBidsAsync(TableView<BidDTO> bidTable, Label messageLabel) {
        Task<List<BidDTO>> task = new Task<>() {
            @Override
            protected List<BidDTO> call() {
                return bidClientService.getMyBids();
            }
        };

        task.setOnSucceeded(event -> {
            List<BidDTO> bids = task.getValue();
            if (bids == null || bids.isEmpty()) {
                bidTable.getItems().clear();
                bidTable.setPlaceholder(new Label("Bạn chưa đặt giá phiên nào."));
                messageLabel.setText("Không có lịch sử đặt giá.");
                return;
            }
            bidTable.setItems(javafx.collections.FXCollections.observableArrayList(bids));
            messageLabel.setText("Đã tải " + bids.size() + " lượt đặt giá.");
        });

        task.setOnFailed(event -> {
            bidTable.getItems().clear();
            bidTable.setPlaceholder(new Label("Không thể tải lịch sử đặt giá."));
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể tải lịch sử đặt giá.";
            messageLabel.setStyle("-fx-text-fill: #b00020;");
            messageLabel.setText(message);
        });

        Thread thread = new Thread(task, "my-bids-history-loader");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void showAllAuctions() {
        currentMode = AuctionListMode.ALL;
        setTitle("Danh sách đấu giá");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyCreatedAuctions() {
        currentMode = AuctionListMode.MY_CREATED;
        setTitle("Phiên đã tạo");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyParticipatedAuctions() {
        currentMode = AuctionListMode.MY_PARTICIPATED;
        setTitle("Phiên đã tham gia");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyWonAuctions() {
        currentMode = AuctionListMode.MY_WON;
        setTitle("Phiên đã thắng");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    private void loadAuctionsForCurrentMode() {
        if (loadingAuctions) {
            return;
        }
        loadingAuctions = true;
        AuctionListMode requestedMode = currentMode;
        StatusFilter requestedStatusFilter = currentStatusFilter;
        setLoading(true);

        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return fetchAuctionsForMode(requestedMode);
            }
        };

        task.setOnSucceeded(event -> {
            if (isCurrentView(requestedMode, requestedStatusFilter)) {
                setData(task.getValue(), requestedStatusFilter);
            }
            loadingAuctions = false;
            setLoading(false);
        });

        task.setOnFailed(event -> {
            setData(List.of(), requestedStatusFilter);
            loadingAuctions = false;
            setLoading(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể tải danh sách đấu giá.";
            AlertUtils.showError("Tải danh sách thất bại", message);
        });

        task.setOnCancelled(event -> {
            loadingAuctions = false;
            setLoading(false);
        });

        Thread thread = new Thread(task, "auction-list-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadAuctionListForRealtime() {
        if (realtimeReloading) {
            return;
        }
        realtimeReloading = true;

        AuctionListMode requestedMode = currentMode;
        StatusFilter requestedStatusFilter = currentStatusFilter;
        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return fetchAuctionsForMode(requestedMode);
            }
        };

        task.setOnSucceeded(event -> {
            realtimeReloading = false;
            if (isCurrentView(requestedMode, requestedStatusFilter)) {
                setData(task.getValue(), requestedStatusFilter);
            }
        });

        task.setOnFailed(event -> {
            realtimeReloading = false;
            Throwable error = task.getException();
            System.err.println("[AuctionMenuController] Failed to reload auctions after realtime update: "
                    + (error == null ? "unknown error" : error.getMessage()));
        });

        task.setOnCancelled(event -> realtimeReloading = false);

        Thread thread = new Thread(task, "auction-list-realtime-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private List<AuctionSummaryDTO> fetchAuctionsForMode(AuctionListMode mode) {
        return switch (mode) {
            case ALL -> auctionClientService.getAllAuctions();
            case MY_CREATED -> auctionClientService.getMyCreatedAuctions();
            case MY_PARTICIPATED -> auctionClientService.getMyParticipatedAuctions();
            case MY_WON -> auctionClientService.getMyWonAuctions();
        };
    }

    private boolean isCurrentView(AuctionListMode requestedMode, StatusFilter requestedStatusFilter) {
        return requestedMode == currentMode && requestedStatusFilter == currentStatusFilter;
    }

    private void setData(List<AuctionSummaryDTO> source, StatusFilter statusFilter) {
        List<AuctionSummaryDTO> filtered = new ArrayList<>();
        if (source != null) {
            for (AuctionSummaryDTO auction : source) {
                if (shouldShowAuction(auction, statusFilter)) {
                    filtered.add(auction);
                }
            }
        }

        filtered.sort(Comparator.comparingLong(AuctionSummaryDTO::getEndTimeMillis).reversed());
        renderAuctionCards(filtered);
    }

    private void renderAuctionCards(List<AuctionSummaryDTO> auctions) {
        auctionGrid.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            Label emptyLabel = new Label("Không có phiên đấu giá phù hợp.");
            emptyLabel.setStyle("-fx-text-fill: #777777; -fx-font-size: 14px;");
            auctionGrid.getChildren().add(emptyLabel);
            return;
        }

        for (AuctionSummaryDTO auction : auctions) {
            auctionGrid.getChildren().add(createAuctionCard(auction));
        }
    }

    private VBox createAuctionCard(AuctionSummaryDTO auction) {
        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setMinHeight(300);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-background-radius: 6;");

        StackPane imageBox = createImagePlaceholder(auction);
        Label title = new Label(safeText(auction.getItemName()));
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #222222;");

        Label price = new Label("Giá hiện tại: " + FormatUtils.currency(auction.getCurrentPrice()));
        price.setStyle("-fx-text-fill: #0B5394; -fx-font-weight: bold;");

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label status = new Label(formatStatus(auction.getStatus()));
        status.setStyle(statusStyle(auction.getStatus()));
        Label itemType = new Label(auction.getItemType() == null ? "" : auction.getItemType().name());
        itemType.setStyle("-fx-text-fill: #666666;");
        statusRow.getChildren().addAll(status, itemType);

        Label time = new Label(formatTimeText(auction));
        time.setWrapText(true);
        time.setStyle("-fx-text-fill: #555555;");

        Label winner = new Label("Người thắng: " + formatWinner(auction));
        winner.setWrapText(true);
        winner.setStyle("-fx-text-fill: #555555;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button actionButton = new Button(actionText(auction));
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setStyle("-fx-background-color: #0B5394; -fx-text-fill: white; -fx-font-weight: bold;");
        actionButton.setDisable(auction.getStatus() == AuctionStatus.PAID);
        actionButton.setOnAction(event -> {
            if (isPayableByCurrentUser(auction)) {
                handlePayAuctionFromCard(auction, actionButton);
                return;
            }
            openAuctionDetail(auction);
        });

        card.getChildren().addAll(imageBox, title, price, statusRow, time, winner, spacer, actionButton);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openAuctionDetail(auction);
            }
        });
        return card;
    }

    private StackPane createImagePlaceholder(AuctionSummaryDTO auction) {
        StackPane imageBox = new StackPane();
        imageBox.setPrefHeight(110);
        imageBox.setMinHeight(110);
        imageBox.setMaxWidth(Double.MAX_VALUE);
        imageBox.setStyle("-fx-background-color: #f1f1f1; -fx-background-radius: 5; -fx-border-color: #e3e3e3; -fx-border-radius: 5;");

        Label placeholder = new Label(auction.getItemType() == null ? "Ảnh sản phẩm" : auction.getItemType().name());
        placeholder.setStyle("-fx-text-fill: #777777; -fx-font-weight: bold;");
        imageBox.getChildren().add(placeholder);
        return imageBox;
    }

    private boolean shouldShowAuction(AuctionSummaryDTO auction, StatusFilter statusFilter) {
        if (auction == null) {
            return false;
        }
        return statusFilter == StatusFilter.ALL || auction.getStatus() == statusFilter.status;
    }

    private String actionText(AuctionSummaryDTO auction) {
        AuctionStatus status = auction.getStatus();
        if (isPayableByCurrentUser(auction)) {
            return "Thanh toán";
        }
        if (status == AuctionStatus.FINISHED) {
            // TODO: show "Thanh toán" when AuctionSummaryDTO exposes winnerId/isCurrentUserWinner.
            return "Xem kết quả";
        }
        if (status == AuctionStatus.PAID) {
            return "Đã thanh toán";
        }
        return "Xem chi tiết";
    }

    private boolean isPayableByCurrentUser(AuctionSummaryDTO auction) {
        if (auction == null || auction.getStatus() != AuctionStatus.FINISHED || auction.getWinnerId() == null) {
            return false;
        }

        UserDTO user = currentUser != null ? currentUser : ClientSession.getCurrentUser();
        return user != null && user.getId() == auction.getWinnerId();
    }

    private void handlePayAuctionFromCard(AuctionSummaryDTO auction, Button actionButton) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán");
        confirm.setHeaderText(null);
        confirm.setContentText("Thanh toán phiên đấu giá \"" + safeText(auction.getItemName())
                + "\" với số tiền " + FormatUtils.currency(auction.getCurrentPrice()) + "?");

        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        actionButton.setDisable(true);
        Task<PayAuctionResponse> task = new Task<>() {
            @Override
            protected PayAuctionResponse call() {
                return walletClientService.payAuction(auction.getAuctionId());
            }
        };

        task.setOnSucceeded(event -> {
            PayAuctionResponse response = task.getValue();
            updateSessionWallet(response);
            renderUserInfo();
            AlertUtils.showInfo("Thanh toán", "Thanh toán thành công.");
            loadAuctionsForCurrentMode();
        });

        task.setOnFailed(event -> {
            actionButton.setDisable(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể thanh toán phiên đấu giá.";
            AlertUtils.showError("Thanh toán thất bại", message);
        });

        task.setOnCancelled(event -> actionButton.setDisable(false));

        Thread thread = new Thread(task, "auction-card-pay-auction");
        thread.setDaemon(true);
        thread.start();
    }

    private String formatStatus(AuctionStatus status) {
        if (status == null) {
            return "Không rõ";
        }
        return switch (status) {
            case OPEN -> "Sắp diễn ra";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }

    private String statusStyle(AuctionStatus status) {
        String base = "-fx-background-radius: 12; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: bold;";
        if (status == AuctionStatus.RUNNING) {
            return base + " -fx-background-color: #e8f5e9; -fx-text-fill: #1b5e20;";
        }
        if (status == AuctionStatus.OPEN) {
            return base + " -fx-background-color: #fff8e1; -fx-text-fill: #8a5a00;";
        }
        if (status == AuctionStatus.PAID) {
            return base + " -fx-background-color: #e3f2fd; -fx-text-fill: #0d47a1;";
        }
        if (status == AuctionStatus.CANCELED) {
            return base + " -fx-background-color: #eeeeee; -fx-text-fill: #555555;";
        }
        return base + " -fx-background-color: #ffebee; -fx-text-fill: #b71c1c;";
    }

    private String formatWinner(AuctionSummaryDTO auction) {
        if (auction == null) {
            return "Chưa xác định";
        }
        String winnerUsername = auction.getWinnerUsername();
        if (winnerUsername != null && !winnerUsername.isBlank()) {
            return winnerUsername;
        }
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            return "Không có";
        }
        return "Chưa xác định";
    }

    private String formatTimeText(AuctionSummaryDTO auction) {
        long endTimeMillis = auction.getEndTimeMillis();
        if (endTimeMillis <= 0) {
            return "Thời gian kết thúc: -";
        }
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            long remainingMillis = endTimeMillis - System.currentTimeMillis();
            if (remainingMillis > 0) {
                return "Còn lại: " + formatRemainingTime(remainingMillis);
            }
        }
        return "Kết thúc: " + END_TIME_FORMATTER.format(Instant.ofEpochMilli(endTimeMillis));
    }

    private String formatRemainingTime(long remainingMillis) {
        Duration duration = Duration.ofMillis(remainingMillis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return String.format("%d ngày %02d giờ", days, hours);
        }
        return String.format("%02d giờ %02d phút", hours, minutes);
    }

    private String formatBidTime(long bidTimeMillis) {
        if (bidTimeMillis <= 0) {
            return "--";
        }
        return END_TIME_FORMATTER.format(Instant.ofEpochMilli(bidTimeMillis));
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "N/A" : text;
    }

    private void showCreateAuctionDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemMenu.fxml"));
            Parent root = loader.load();
            AuctionItemMenuController controller = loader.getController();

            Stage owner = (Stage) auctionGrid.getScene().getWindow();
            Stage dialog = new Stage();
            dialog.setTitle("Tạo phiên đấu giá");
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(true);
            dialog.setMinWidth(760);
            dialog.setMinHeight(620);
            dialog.setScene(new Scene(root, 820, 680));

            controller.setOnCancel(dialog::close);
            controller.setOnAuctionCreated(() -> {
                dialog.close();
                loadAuctionsForCurrentMode();
            });

            dialog.showAndWait();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở màn tạo phiên đấu giá.");
        }
    }

    private void selectStatusFilter(StatusFilter statusFilter) {
        currentStatusFilter = statusFilter;
        if (statusToggleGroup == null) {
            return;
        }

        ToggleButton target = switch (statusFilter) {
            case ALL -> allAuctionsToggle;
            case OPEN -> openAuctionsToggle;
            case RUNNING -> runningAuctionsToggle;
            case FINISHED -> finishedAuctionsToggle;
        };
        target.setSelected(true);
        styleStatusToggles();
    }

    private void setTitle(String title) {
        titleLabel.setText(title);
    }

    private void updateSidebarSelection() {
        styleSidebarButton(allAuctionsButton, currentMode == AuctionListMode.ALL);
        styleSidebarButton(myParticipatedButton, currentMode == AuctionListMode.MY_PARTICIPATED);
        styleSidebarButton(myWonButton, currentMode == AuctionListMode.MY_WON);
        styleSidebarButton(myCreatedButton, currentMode == AuctionListMode.MY_CREATED);
        styleSidebarButton(createAuctionButton, false);
        styleSidebarButton(myBidsButton, false);
    }

    private void styleSidebarButton(Button button, boolean selected) {
        String baseStyle = "-fx-border-color: #0B5394; -fx-border-radius: 4;";
        if (selected) {
            button.setStyle(baseStyle + " -fx-background-color: #0B5394; -fx-text-fill: white;");
            return;
        }
        button.setStyle(baseStyle + " -fx-background-color: white; -fx-text-fill: #0B5394;");
    }

    private void styleStatusToggles() {
        styleStatusToggle(allAuctionsToggle, currentStatusFilter == StatusFilter.ALL);
        styleStatusToggle(openAuctionsToggle, currentStatusFilter == StatusFilter.OPEN);
        styleStatusToggle(runningAuctionsToggle, currentStatusFilter == StatusFilter.RUNNING);
        styleStatusToggle(finishedAuctionsToggle, currentStatusFilter == StatusFilter.FINISHED);
    }

    private void styleStatusToggle(ToggleButton button, boolean selected) {
        String baseStyle = "-fx-border-color: #0B5394; -fx-border-radius: 4;";
        if (selected) {
            button.setStyle(baseStyle + " -fx-background-color: #0B5394; -fx-text-fill: white;");
            return;
        }
        button.setStyle(baseStyle + " -fx-background-color: white; -fx-text-fill: #0B5394;");
    }

    private void openAuctionDetail(AuctionSummaryDTO auction) {
        if (auction == null) {
            return;
        }

        try {
            Stage stage = (Stage) auctionGrid.getScene().getWindow();
            AuctionDetailController controller =
                    SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionDetailView.fxml");
            controller.setInitialAuction(auction);
            stage.setMaximized(true);
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở chi tiết phiên đấu giá.");
        }

    }

    private void openAuctionDetailById(int auctionId) {
        if (auctionId <= 0) {
            return;
        }

        try {
            Stage stage = (Stage) auctionGrid.getScene().getWindow();
            AuctionDetailController controller =
                    SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionDetailView.fxml");
            controller.setAuctionId(auctionId);
            stage.setMaximized(true);
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở chi tiết phiên đấu giá.");
        }
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
                if (!(payload instanceof AuctionUpdatedEvent)) {
                    return;
                }

                reloadAuctionListForRealtime();
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

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        auctionScrollPane.setDisable(loading);
        allAuctionsToggle.setDisable(loading);
        openAuctionsToggle.setDisable(loading);
        runningAuctionsToggle.setDisable(loading);
        finishedAuctionsToggle.setDisable(loading);
        allAuctionsButton.setDisable(loading);
        myParticipatedButton.setDisable(loading);
        myWonButton.setDisable(loading);
        myCreatedButton.setDisable(loading);
    }
}
