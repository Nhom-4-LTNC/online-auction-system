package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.client.session.ClientSession;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;
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

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final Client client = Client.getInstance();

    @FXML private ToggleButton allAuctionsToggle;
    @FXML private ToggleButton openAuctionsToggle;
    @FXML private ToggleButton runningAuctionsToggle;
    @FXML private ToggleButton finishedAuctionsToggle;
    @FXML private ListView<AuctionSummaryDTO> auctionListView;
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
        topUpButton.setOnAction(event -> AlertUtils.showInfo(
                "Nạp tiền",
                "Chức năng nạp tiền sẽ được bổ sung sau."
        ));
        setupSidebarActions();

        registerRealtimeListener();
        auctionListView.sceneProperty().addListener((observable, oldScene, newScene) -> {
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
            ClientSession.clear();
            SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể quay về màn hình đăng nhập.");
        }
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
        createAuctionButton.setOnAction(event -> openScene("/fxml/ItemMenu.fxml", "Không thể mở màn tạo phiên đấu giá."));
        myBidsButton.setOnAction(event -> AlertUtils.showInfo(
                "Lịch sử đặt giá",
                "Màn hình lịch sử đặt giá cá nhân chưa được triển khai."
        ));
        updateSidebarSelection();
    }

    private void setupAuctionList() {
        auctionListView.setCellFactory(listView -> {
            AuctionSummaryCell cell = new AuctionSummaryCell();
            cell.setOnMouseClicked(event -> handleCellClick(cell));
            return cell;
        });
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
            if (requestedMode == currentMode && requestedStatusFilter == currentStatusFilter) {
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
            if (requestedMode == currentMode && requestedStatusFilter == currentStatusFilter) {
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
        ObservableList<AuctionSummaryDTO> data = FXCollections.observableArrayList(filtered);
        auctionListView.setItems(data);
    }

    private boolean shouldShowAuction(AuctionSummaryDTO auction, StatusFilter statusFilter) {
        if (auction == null) {
            return false;
        }
        return statusFilter == StatusFilter.ALL || auction.getStatus() == statusFilter.status;
    }

    private void openScene(String fxmlPath, String errorMessage) {
        try {
            Stage stage = (Stage) auctionListView.getScene().getWindow();
            SceneUtils.switchScene(stage, fxmlPath);
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", errorMessage);
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
        String baseStyle = "-fx-border-color: #e79316; -fx-border-radius: 4;";
        if (selected) {
            button.setStyle(baseStyle + " -fx-background-color: #e79316; -fx-text-fill: white;");
            return;
        }
        button.setStyle(baseStyle + " -fx-background-color: white; -fx-text-fill: #e79316;");
    }

    private void styleStatusToggles() {
        styleStatusToggle(allAuctionsToggle, currentStatusFilter == StatusFilter.ALL);
        styleStatusToggle(openAuctionsToggle, currentStatusFilter == StatusFilter.OPEN);
        styleStatusToggle(runningAuctionsToggle, currentStatusFilter == StatusFilter.RUNNING);
        styleStatusToggle(finishedAuctionsToggle, currentStatusFilter == StatusFilter.FINISHED);
    }

    private void styleStatusToggle(ToggleButton button, boolean selected) {
        String baseStyle = "-fx-border-color: #e79316; -fx-border-radius: 4;";
        if (selected) {
            button.setStyle(baseStyle + " -fx-background-color: #e79316; -fx-text-fill: white;");
            return;
        }
        button.setStyle(baseStyle + " -fx-background-color: white; -fx-text-fill: #e79316;");
    }

    private void handleCellClick(ListCell<AuctionSummaryDTO> cell) {
        if (cell == null || cell.isEmpty()) {
            return;
        }

        AuctionSummaryDTO selected = cell.getItem();
        if (selected == null) {
            return;
        }

        try {
            Stage stage = (Stage) auctionListView.getScene().getWindow();
            AuctionDetailController controller =
                    SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionDetailView.fxml");
            controller.setInitialAuction(selected);
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
        auctionListView.setDisable(loading);
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
