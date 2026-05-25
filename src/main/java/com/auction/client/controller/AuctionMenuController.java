package com.auction.client.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.auction.client.network.Client;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.GetAllAuctionResponse;
import com.auction.shared.protocol.auction.GetAuctionsByTypeRequest;
import com.auction.shared.protocol.auction.GetAuctionsByTypeResponse;
import com.auction.shared.util.SceneUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class AuctionMenuController {

    private final Client client = Client.getInstance();

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;

    @FXML private ListView<AuctionSummaryDTO> auctionListView;

    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ItemType currentType = ItemType.ELECTRONICS;

    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        electronicsButton.setToggleGroup(group);
        artButton.setToggleGroup(group);
        vehicleButton.setToggleGroup(group);
        electronicsButton.setSelected(true);

        electronicsButton.setUserData(ItemType.ELECTRONICS);
        artButton.setUserData(ItemType.ART);
        vehicleButton.setUserData(ItemType.VEHICLE);

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            Object ud = ((RadioButton) newV.getToggleGroup().getSelectedToggle()).getUserData();
            if (ud instanceof ItemType t) {
                currentType = t;
                load();
            }
        });

        auctionListView.setCellFactory(lv -> {
            ListCell<AuctionSummaryDTO> cell = new ListCell<>() {
                @Override
                protected void updateItem(AuctionSummaryDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        AuctionStatus st = item.getStatus();
                        setText("#" + item.getAuctionId() + " | " + item.getItemName() + " | "
                                + item.getCurrentPrice() + " | " + (st == null ? "" : st));
                    }
                }
            };

            cell.setOnMouseClicked(e -> {
                if (cell.isEmpty()) return;
                AuctionSummaryDTO selected = cell.getItem();
                if (selected == null) return;

                try {
                    var url = getClass().getResource("/fxml/ItemAuction.fxml");
                    if (url == null) {
                        System.err.println("[AuctionMenuController] Resource not found: /fxml/ItemAuction.fxml");
                        return;
                    }

                    FXMLLoader loader = new FXMLLoader(url);
                    Parent root = loader.load();
                    Object controller = loader.getController();
                    if (controller != null) {
                        try {
                            var m = controller.getClass().getMethod("setAuctionId", int.class);
                            m.invoke(controller, selected.getAuctionId());
                        } catch (Exception ignored) {
                            // fallback: allow setAuctionId(Integer)
                            try {
                                var m = controller.getClass().getMethod("setAuctionId", Integer.class);
                                m.invoke(controller, selected.getAuctionId());
                            } catch (Exception ignored2) {
                                // ignore if setter not found
                            }
                        }
                    }


                    Stage stage = (Stage) auctionListView.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            return cell;
        });

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);

        // load initial data
        load();
    }

    public void handleRefresh(ActionEvent event) {
        load();
    }

    public void handleBack(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        // Ensure connected
        if (!client.isConnected()) {
            client.connect();
        }

        client.setOnMessageReceived(message -> {
            if (!(message instanceof Response<?> response)) return;
            if (response.getAction() != ActionType.GET_AUCTIONS_BY_TYPE) return;
            if (!response.isSuccess()) {
                System.err.println("[AuctionMenuController] Failed: " + response.getErrorMessage());
                return;
            }
            if (!(response.getPayload() instanceof GetAuctionsByTypeResponse payload)) return;

            List<AuctionSummaryDTO> filtered = payload.getAuctions();

            ObservableList<AuctionSummaryDTO> data = FXCollections.observableArrayList(filtered);
            Platform.runLater(() -> auctionListView.setItems(data));
        });

        // Request all auctions then filter client-side by currentType
        Request<java.io.Serializable> request = new Request<>(ActionType.GET_AUCTIONS_BY_TYPE, new GetAuctionsByTypeRequest(currentType));

        client.sendMessage(request);
    }
}

