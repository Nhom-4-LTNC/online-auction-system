package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.auction.client.network.Client;
import com.auction.client.service.AuthClientService;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AdminMenuController implements Initializable {
    private static final DateTimeFormatter TABLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final Client client = Client.getInstance();
    private final AuthClientService authClientService = new AuthClientService();

    // AdminScreen.fxml chỉ cần TableView phiên đấu giá.
    @FXML private TableView<AuctionSummaryDTO> auctionsTableView;

    @FXML private TableColumn<AuctionSummaryDTO, Integer> idColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Object> imageColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> nameColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> creatorColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> currentPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> startPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> remainingTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> statusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> actionsColumn;

    @FXML private Button auctionAllSessionsButton;
    @FXML private Button viewCanceledAuctionsButton;

    @FXML private Button logoutButton;
    @FXML private Label adminWelcomeLabel;

    private final com.auction.client.service.AuctionClientService auctionClientService = new com.auction.client.service.AuctionClientService();
    private volatile boolean loadingAuctions = false;
    private long auctionLoadVersion = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!client.isConnected()) {
            client.connect();
        }

        if (!ClientSession.isAdmin()) {
            AlertUtils.showError("Không có quyền", "Chỉ Admin mới được vào trang này.");
            return;
        }

        renderAdminInfo();
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

    private void renderAdminInfo() {
        if (adminWelcomeLabel == null) {
            return;
        }

        var user = ClientSession.getCurrentUser();
        if (user != null) {
            adminWelcomeLabel.setText("Chào, " + user.getUsername() + " (ID: " + user.getId() + ")");
        } else {
            adminWelcomeLabel.setText("Admin");
        }
    }

    private void setupTableColumns() {
        if (auctionsTableView == null) return;

        hideColumn(imageColumn);
        hideColumn(creatorColumn);
        hideColumn(startPriceColumn);

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
                    new javafx.beans.property.SimpleStringProperty(
                            FormatUtils.currency(cell.getValue().getCurrentPrice())));
        }

        if (remainingTimeColumn != null) {
            remainingTimeColumn.setText("Thời gian");
            remainingTimeColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(formatTimeRange(cell.getValue())));
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(formatStatus(cell.getValue().getStatus())));
        }

        if (actionsColumn != null) {
            actionsColumn.setCellFactory(column -> new TableCell<>() {
                private final Button detailButton = new Button("Xem chi tiết");

                {
                    detailButton.setStyle("-fx-background-color: #0B5394; -fx-text-fill: white; -fx-font-weight: 700;");
                    detailButton.setOnAction(event -> {
                        AuctionSummaryDTO auction = getTableView().getItems().get(getIndex());
                        openAuctionDetail(auction);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    setGraphic(detailButton);
                }
            });
        }

        auctionsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void hideColumn(TableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        column.setVisible(false);
    }

    private String formatTimeRange(AuctionSummaryDTO auction) {
        if (auction == null) {
            return "";
        }
        String start = formatTime(auction.getStartTimeMillis());
        String end = formatTime(auction.getEndTimeMillis());
        if (start.isBlank() && end.isBlank()) {
            return "Chưa rõ";
        }
        if (start.isBlank()) {
            return "Kết thúc: " + end;
        }
        if (end.isBlank()) {
            return "Bắt đầu: " + start;
        }
        return start + " - " + end;
    }

    private String formatTime(long epochMillis) {
        if (epochMillis <= 0) {
            return "";
        }
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(TABLE_TIME_FORMATTER);
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

    private void openAuctionDetail(AuctionSummaryDTO auction) {
        if (auction == null || auctionsTableView == null || auctionsTableView.getScene() == null) {
            return;
        }

        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
            var root = loader.load();
            AuctionDetailController controller = loader.getController();

            javafx.stage.Stage owner = (javafx.stage.Stage) auctionsTableView.getScene().getWindow();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Chi tiết phiên đấu giá #" + auction.getAuctionId());
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setResizable(true);
            dialog.setMinWidth(980);
            dialog.setMinHeight(680);
            dialog.setScene(new javafx.scene.Scene((javafx.scene.Parent) root, 1180, 720));
            controller.setOnBack(dialog::close);
            controller.setInitialAuction(auction);
            dialog.setOnHidden(event -> {
                controller.cleanup();
                refreshAuctions();
            });
            dialog.showAndWait();
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở chi tiết phiên đấu giá.");
        }
    }

    private void refreshAuctions() {
        loadAuctionsAsync(null);
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
        loadAuctionsAsync(AuctionStatus.CANCELED);
    }

    private void loadAuctionsAsync(AuctionStatus statusFilter) {
        if (loadingAuctions || auctionsTableView == null) {
            return;
        }

        loadingAuctions = true;
        long requestVersion = ++auctionLoadVersion;
        setAuctionTableLoading(true);

        Task<List<AuctionSummaryDTO>> task = new Task<>() {
            @Override
            protected List<AuctionSummaryDTO> call() {
                List<AuctionSummaryDTO> auctions = auctionClientService.getAllAuctions();
                if (auctions == null) {
                    return List.of();
                }
                if (statusFilter == null) {
                    return auctions;
                }
                return auctions.stream()
                        .filter(auction -> auction != null && auction.getStatus() == statusFilter)
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            if (requestVersion == auctionLoadVersion) {
                auctionsTableView.setItems(FXCollections.observableArrayList(task.getValue()));
            }
            loadingAuctions = false;
            setAuctionTableLoading(false);
        });

        task.setOnFailed(event -> {
            loadingAuctions = false;
            setAuctionTableLoading(false);
            Throwable error = task.getException();
            AlertUtils.showError("Lỗi", "Không thể tải danh sách phiên đấu giá: "
                    + (error == null ? "Không rõ lỗi" : error.getMessage()));
        });

        task.setOnCancelled(event -> {
            loadingAuctions = false;
            setAuctionTableLoading(false);
        });

        Thread thread = new Thread(task, "admin-auction-list-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void setAuctionTableLoading(boolean loading) {
        if (auctionAllSessionsButton != null) {
            auctionAllSessionsButton.setDisable(loading);
        }
        if (viewCanceledAuctionsButton != null) {
            viewCanceledAuctionsButton.setDisable(loading);
        }
        if (auctionsTableView != null) {
            auctionsTableView.setDisable(loading);
        }
    }

    @FXML
    public void viewAllUsers(ActionEvent event) {
        openUsersView(false);
    }

    @FXML
    public void viewBannedUsers(ActionEvent event) {
        openUsersView(true);
    }

    private void openUsersView(boolean bannedOnly) {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AdminUsersView.fxml"));
            var root = loader.load();

            var controller = loader.getController();
            if (controller instanceof AdminUsersViewController c) {
                c.setBannedOnly(bannedOnly);
            }

            javafx.stage.Stage stage = (javafx.stage.Stage) auctionsTableView.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene((javafx.scene.Parent) root));
        } catch (Exception e) {
            AlertUtils.showError("Lỗi điều hướng", e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        logoutServerSideAsync();
        ClientSession.clear();
        try {
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
        }, "admin-menu-logout");
        thread.setDaemon(true);
        thread.start();
    }
}


