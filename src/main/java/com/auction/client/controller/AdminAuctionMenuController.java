package com.auction.client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Admin riêng để xem auction list.
 * Dùng chung logic hiển thị như AuctionMenuController (nhưng tách controller để có screen riêng cho admin).
 */
public class AdminAuctionMenuController {

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final Client client = Client.getInstance();

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;
    @FXML private ListView<AuctionSummaryDTO> auctionListView;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ItemType currentType = ItemType.ELECTRONICS;
    private Consumer<Response<?>> auctionUpdatedListener;
    private volatile boolean realtimeReloading = false;

    @FXML
    public void initialize() {
        if (!client.isConnected()) {
            client.connect();
        }

        setupTypeFilter();
        setupAuctionList();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);

        registerRealtimeListener();

        loadAuctions();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/AdminScreen.fxml");
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Navigation error", "Cannot open admin screen: " + e.getMessage());
        }
    }

    private void setupTypeFilter() {
        ToggleGroup group = new ToggleGroup();
        electronicsButton.setToggleGroup(group);
        artButton.setToggleGroup(group);
        vehicleButton.setToggleGroup(group);

        electronicsButton.setUserData(ItemType.ELECTRONICS);
        artButton.setUserData(ItemType.ART);
        vehicleButton.setUserData(ItemType.VEHICLE);
        electronicsButton.setSelected(true);

        group.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || !(newValue.getUserData() instanceof ItemType selectedType)) {
                return;
            }

            currentType = selectedType;
            loadAuctions();
        });
    }

    private void setupAuctionList() {
        auctionListView.setCellFactory(listView -> {
            AuctionSummaryCell cell = new AuctionSummaryCell();
            cell.setOnMouseClicked(event -> handleCellClick(cell));
            return cell;
        });
    }

    private void loadAuctions() {
        ItemType requestedType = currentType;
        setLoading(true);

        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return auctionClientService.getAuctionsByType(requestedType);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestedType == currentType) {
                setData(task.getValue(), requestedType);
            }
            setLoading(false);
        });

        task.setOnFailed(event -> {
            setData(List.of(), requestedType);
            setLoading(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Cannot load auctions.";
            AlertUtils.showError("Load auctions failed", message);
        });

        Thread thread = new Thread(task, "admin-auction-list-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadAuctionListForRealtime() {
        if (realtimeReloading) return;
        realtimeReloading = true;

        ItemType requestedType = currentType;
        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                return auctionClientService.getAuctionsByType(requestedType);
            }
        };

        task.setOnSucceeded(event -> {
            realtimeReloading = false;
            if (requestedType == currentType) {
                setData(task.getValue(), requestedType);
            }
        });

        task.setOnFailed(event -> {
            realtimeReloading = false;
            Throwable error = task.getException();
            System.err.println("[AdminAuctionMenuController] realtime reload failed: "
                    + (error == null ? "unknown error" : error.getMessage()));
        });

        task.setOnCancelled(event -> realtimeReloading = false);

        Thread thread = new Thread(task, "admin-auction-list-realtime-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void setData(List<AuctionSummaryDTO> source, ItemType type) {
        List<AuctionSummaryDTO> filtered = new ArrayList<>();
        if (source != null) {
            for (AuctionSummaryDTO auction : source) {
                if (auction != null && type.equals(auction.getItemType())) {
                    filtered.add(auction);
                }
            }
        }

        filtered.sort(Comparator.comparingLong(AuctionSummaryDTO::getEndTimeMillis).reversed());
        ObservableList<AuctionSummaryDTO> data = FXCollections.observableArrayList(filtered);
        auctionListView.setItems(data);
    }

    private void handleCellClick(ListCell<AuctionSummaryDTO> cell) {
        if (cell == null || cell.isEmpty()) return;
        AuctionSummaryDTO selected = cell.getItem();
        if (selected == null) return;

        try {
            Stage stage = (Stage) auctionListView.getScene().getWindow();
            AuctionDetailController controller =
                    SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionDetailView.fxml");
            controller.setInitialAuction(selected);
            cleanup();
        } catch (IOException e) {
            AlertUtils.showError("Navigation error", "Cannot open auction detail: " + e.getMessage());
        }
    }

    private void registerRealtimeListener() {
        unregisterRealtimeListener();

        auctionUpdatedListener = response -> {
            if (response == null || response.getAction() != ActionType.AUCTION_UPDATED) return;

            Object payload = response.getPayload();
            if (!(payload instanceof AuctionUpdatedEvent)) return;

            // đảm bảo chạy FX thread
            Platform.runLater(this::reloadAuctionListForRealtime);
        };

        client.addEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
    }

    private void unregisterRealtimeListener() {
        if (auctionUpdatedListener != null) {
            client.removeEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
            auctionUpdatedListener = null;
        }
    }

    public void cleanup() {
        unregisterRealtimeListener();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        auctionListView.setDisable(loading);
    }
}

