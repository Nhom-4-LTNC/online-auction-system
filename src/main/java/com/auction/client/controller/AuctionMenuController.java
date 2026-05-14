package com.auction.client.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.auction.model.auction.AuctionListItem;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.ItemType;
import com.auction.service.AuctionService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class
AuctionMenuController {

    private final AuctionService auctionService = AuctionService.getInstance();

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;

    @FXML private ListView<AuctionListItem> auctionListView;

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

        auctionListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AuctionListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    AuctionStatus st = item.getStatus();
                    setText("#" + item.getAuctionId() + " | " + item.getItemName() + " | " + item.getCurrentPrice() + " | " + (st == null ? "" : st));
                }
            }
        });

        load();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);
    }

    private void handleRefresh(ActionEvent event) {
        load();
    }

    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/HomeScreen.fxml")));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            // ignore for now
        }
    }

    private void load() {
        List<AuctionListItem> list = auctionService.getAuctionListByType(currentType);
        list.sort(Comparator.comparingLong(AuctionListItem::getEndTime).reversed());
        ObservableList<AuctionListItem> data = FXCollections.observableArrayList(list);
        auctionListView.setItems(data);
    }
}

