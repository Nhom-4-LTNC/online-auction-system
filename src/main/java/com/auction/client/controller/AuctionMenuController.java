package com.auction.client.controller;

import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.ItemType;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AuctionMenuController {

    private final AuctionClientService auctionClientService = new AuctionClientService();

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;
    @FXML private ListView<AuctionSummaryDTO> auctionListView;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ItemType currentType = ItemType.ELECTRONICS;

    @FXML
    public void initialize() {
        setupTypeFilter();
        setupAuctionList();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);

        loadAuctions();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
        } catch (IOException e) {
            AlertUtils.showError("Navigation error", "Cannot open home screen: " + e.getMessage());
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

        Thread thread = new Thread(task, "auction-list-loader");
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
            controller.setAuctionId(selected.getAuctionId());
        } catch (IOException e) {
            AlertUtils.showError("Navigation error", "Cannot open auction detail: " + e.getMessage());
        }
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        auctionListView.setDisable(loading);
    }
}
