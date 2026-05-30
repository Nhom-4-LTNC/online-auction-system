package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.auction.client.network.Client;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AdminMenuController implements Initializable {

    private final Client client = Client.getInstance();

    // AdminScreen.fxml chỉ cần TableView phiên đấu giá.
    @FXML private TableView<AuctionSummaryDTO> auctionsTableView;

    @FXML private TableColumn<AuctionSummaryDTO, Integer> idColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Object> imageColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> nameColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> creatorColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Double> currentPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Double> startPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> remainingTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> statusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> actionsColumn;

    @FXML private Button auctionAllSessionsButton;
    @FXML private Button viewCanceledAuctionsButton;

    @FXML private Button logoutButton;

    private final com.auction.client.service.AuctionClientService auctionClientService = new com.auction.client.service.AuctionClientService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!client.isConnected()) {
            client.connect();
        }

        if (!ClientSession.isAdmin()) {
            AlertUtils.showError("Không có quyền", "Chỉ Admin mới được vào trang này.");
            return;
        }

        setupTableColumns();
        refreshAuctions();

        // Gắn handler cho các nút nếu FXML gọi @FXML method không tồn tại.
        // (Nếu FXML đã map đúng onAction="#refresh" ... thì không cần.)
        if (auctionAllSessionsButton != null) {
            auctionAllSessionsButton.setOnAction(e -> viewAllAuctions(e));
        }
        if (viewCanceledAuctionsButton != null) {
            viewCanceledAuctionsButton.setOnAction(this::viewCanceledAuctions);
        }

    }

    private void setupTableColumns() {
        if (auctionsTableView == null) return;

        // AuctionSummaryDTO hiện chỉ có các field: auctionId, itemId, itemName, itemType,
        // currentPrice, endTimeMillis, status, winnerId.
        // Vì AdminScreen.fxml khai báo thêm nhiều cột khác (image/creator/start/remaining/actions)
        // nên tạm thời chỉ bind những cột có dữ liệu thực sự.

        if (idColumn != null) {
            idColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getAuctionId()).asObject());
        }

        if (nameColumn != null) {
            nameColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(cell.getValue().getItemName()));
        }

        if (currentPriceColumn != null) {
            currentPriceColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getCurrentPrice()).asObject());
        }

        if (remainingTimeColumn != null) {
            // Remaining time = endTimeMillis - now
            remainingTimeColumn.setCellValueFactory(cell -> {
                long remainingMillis = cell.getValue().getEndTimeMillis() - System.currentTimeMillis();
                long remainingSec = Math.max(0, remainingMillis / 1000);
                return new javafx.beans.property.SimpleStringProperty(remainingSec + "s");
            });
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().name()));
        }

        auctionsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }


    private void refreshAuctions() {
        try {
            List<AuctionSummaryDTO> auctions = auctionClientService.getAllAuctions();
            auctionsTableView.setItems(FXCollections.observableArrayList(auctions));
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Không thể tải tất cả phiên đấu giá: " + e.getMessage());
        }
    }

    @FXML
    public void viewAllAuctions(ActionEvent event) {
        refreshAuctions();
    }


    // AdminScreen.fxml dùng onAction="#refresh" cho một số nút.
    @FXML
    public void refresh(ActionEvent event) {
        refreshAuctions();
    }


    @FXML
    public void viewCanceledAuctions(ActionEvent event) {
        try {
            List<AuctionSummaryDTO> auctions = auctionClientService.getAllAuctions();
            auctions.removeIf(a -> a == null || a.getStatus() != AuctionStatus.CANCELED);
            auctionsTableView.setItems(FXCollections.observableArrayList(auctions));
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Không thể tải phiên đã hủy: " + e.getMessage());
        }
    }

    // TODO: Hiện tại AdminScreen.fxml có các onAction này nhưng controller chưa triển khai.
    // Thêm stub để tránh FXMLLoader crash khi đăng nhập ADMIN.
    @FXML
    public void viewAllUsers(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/AdminUsersView.fxml");
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", e.getMessage());
        }
    }

    @FXML
    public void viewBannedUsers(ActionEvent event) {
        // Load AdminUsersView.fxml thủ công để truyền tham số bannedOnly sang controller.
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AdminUsersView.fxml"));
            var root = loader.load();

            var controller = loader.getController();
            if (controller instanceof AdminUsersViewController c) {
                c.setBannedOnly(true);
            }

            javafx.stage.Stage stage = (javafx.stage.Stage) auctionsTableView.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene((javafx.scene.Parent) root));

        } catch (Exception e) {
            AlertUtils.showError("Lỗi điều hướng", e.getMessage());
        }
    }



    @FXML
    public void back(ActionEvent event) {
        try {
            ClientSession.clear();
        } catch (Exception ignored) {
        }

        try {
            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
        } catch (IOException ignored) {
        }
    }
}


