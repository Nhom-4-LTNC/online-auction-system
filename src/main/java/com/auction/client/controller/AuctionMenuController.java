package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.ItemType;
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
import javafx.scene.control.RadioButton;
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
        BY_ITEM_TYPE,
        MY_CREATED,
        MY_PARTICIPATED,
        MY_WON
    }

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final Client client = Client.getInstance();

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;
    @FXML private ListView<AuctionSummaryDTO> auctionListView;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button allAuctionsButton;
    @FXML private Button createAuctionButton;
    @FXML private Button myBidsButton;
    @FXML private Button myParticipatedButton;
    @FXML private Button myWonButton;
    @FXML private Button myCreatedButton;
    @FXML private Button paymentButton;
    @FXML private Label titleLabel;

    private ItemType currentType = ItemType.ELECTRONICS;
    private AuctionListMode currentMode = AuctionListMode.ALL;
    private ToggleGroup itemTypeGroup;
    private Consumer<Response<?>> auctionUpdatedListener;
    private boolean realtimeListenerRegistered = false;
    private volatile boolean loadingAuctions = false;
    private volatile boolean realtimeReloading = false;

    @FXML
    public void initialize() {
        setupTypeFilter();
        setupAuctionList();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);
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
            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở màn hình chính.");
        }
    }

    private void setupTypeFilter() {
        itemTypeGroup = new ToggleGroup();
        electronicsButton.setToggleGroup(itemTypeGroup);
        artButton.setToggleGroup(itemTypeGroup);
        vehicleButton.setToggleGroup(itemTypeGroup);

        electronicsButton.setUserData(ItemType.ELECTRONICS);
        artButton.setUserData(ItemType.ART);
        vehicleButton.setUserData(ItemType.VEHICLE);

        itemTypeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || !(newValue.getUserData() instanceof ItemType selectedType)) {
                return;
            }

            currentType = selectedType;
            currentMode = AuctionListMode.BY_ITEM_TYPE;
            setTitle("Danh sách đấu giá");
            updateSidebarSelection();
            loadAuctionsForCurrentMode();
        });
    }

    private void setupSidebarActions() {
        allAuctionsButton.setOnAction(event -> showAllAuctions());
        myParticipatedButton.setOnAction(event -> showMyParticipatedAuctions());
        myWonButton.setOnAction(event -> showMyWonAuctions());
        myCreatedButton.setOnAction(event -> showMyCreatedAuctions());
        createAuctionButton.setOnAction(event -> openScene("/fxml/ItemMenu.fxml", "Không thể mở màn tạo phiên đấu giá."));
        paymentButton.setOnAction(event -> openScene("/fxml/FinanceMenu.fxml", "Không thể mở màn thanh toán."));
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
        clearTypeSelection();
        setTitle("Danh sách đấu giá");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyCreatedAuctions() {
        currentMode = AuctionListMode.MY_CREATED;
        clearTypeSelection();
        setTitle("Phiên đã tạo");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyParticipatedAuctions() {
        currentMode = AuctionListMode.MY_PARTICIPATED;
        clearTypeSelection();
        setTitle("Phiên đã tham gia");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    @FXML
    public void showMyWonAuctions() {
        currentMode = AuctionListMode.MY_WON;
        clearTypeSelection();
        setTitle("Phiên đã thắng");
        updateSidebarSelection();
        loadAuctionsForCurrentMode();
    }

    private void loadAuctionsForCurrentMode() {
        if (loadingAuctions) {
            return;
        }
        loadingAuctions = true;
        ItemType requestedType = currentType;
        AuctionListMode requestedMode = currentMode;
        setLoading(true);

        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return fetchAuctionsForMode(requestedMode, requestedType);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestedMode == currentMode && requestedType == currentType) {
                setData(task.getValue(), requestedMode, requestedType);
            }
            loadingAuctions = false;
            setLoading(false);
        });

        task.setOnFailed(event -> {
            setData(List.of(), requestedMode, requestedType);
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

        ItemType requestedType = currentType;
        AuctionListMode requestedMode = currentMode;
        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return fetchAuctionsForMode(requestedMode, requestedType);
            }
        };

        task.setOnSucceeded(event -> {
            realtimeReloading = false;
            if (requestedMode == currentMode && requestedType == currentType) {
                setData(task.getValue(), requestedMode, requestedType);
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

    private List<AuctionSummaryDTO> fetchAuctionsForMode(AuctionListMode mode, ItemType type) {
        return switch (mode) {
            case ALL -> auctionClientService.getAllAuctions();
            case BY_ITEM_TYPE -> auctionClientService.getAuctionsByType(type);
            case MY_CREATED -> auctionClientService.getMyCreatedAuctions();
            case MY_PARTICIPATED -> auctionClientService.getMyParticipatedAuctions();
            case MY_WON -> auctionClientService.getMyWonAuctions();
        };
    }

    private void setData(List<AuctionSummaryDTO> source, AuctionListMode mode, ItemType type) {
        List<AuctionSummaryDTO> filtered = new ArrayList<>();
        if (source != null) {
            for (AuctionSummaryDTO auction : source) {
                if (shouldShowAuction(auction, mode, type)) {
                    filtered.add(auction);
                }
            }
        }

        filtered.sort(Comparator.comparingLong(AuctionSummaryDTO::getEndTimeMillis).reversed());
        ObservableList<AuctionSummaryDTO> data = FXCollections.observableArrayList(filtered);
        auctionListView.setItems(data);
    }

    private boolean shouldShowAuction(AuctionSummaryDTO auction, AuctionListMode mode, ItemType type) {
        if (auction == null) {
            return false;
        }
        if (mode == AuctionListMode.BY_ITEM_TYPE) {
            return type.equals(auction.getItemType());
        }
        return true;
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

    private void clearTypeSelection() {
        if (itemTypeGroup != null) {
            itemTypeGroup.selectToggle(null);
        }
    }

    private void setTitle(String title) {
        titleLabel.setText(title);
    }

    private void updateSidebarSelection() {
        styleSidebarButton(allAuctionsButton, currentMode == AuctionListMode.ALL
                || currentMode == AuctionListMode.BY_ITEM_TYPE);
        styleSidebarButton(myParticipatedButton, currentMode == AuctionListMode.MY_PARTICIPATED);
        styleSidebarButton(myWonButton, currentMode == AuctionListMode.MY_WON);
        styleSidebarButton(myCreatedButton, currentMode == AuctionListMode.MY_CREATED);
        styleSidebarButton(createAuctionButton, false);
        styleSidebarButton(myBidsButton, false);
        styleSidebarButton(paymentButton, false);
    }

    private void styleSidebarButton(Button button, boolean selected) {
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
        electronicsButton.setDisable(loading);
        artButton.setDisable(loading);
        vehicleButton.setDisable(loading);
        allAuctionsButton.setDisable(loading);
        myParticipatedButton.setDisable(loading);
        myWonButton.setDisable(loading);
        myCreatedButton.setDisable(loading);
    }
}
