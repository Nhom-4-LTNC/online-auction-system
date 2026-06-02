package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.auction.client.network.Client;
import com.auction.client.service.AdminClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.protocol.admin.ApplyBanResponse;
import com.auction.shared.protocol.admin.RemoveBanResponse;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminUsersViewController implements Initializable {

    private final Client client = Client.getInstance();
    private final AdminClientService adminClientService = new AdminClientService();

    @FXML private Button backButton;

    @FXML private TableView<UserDTO> usersTableView;

    @FXML private TableColumn<UserDTO, Integer> idColumn;
    @FXML private TableColumn<UserDTO, String> usernameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> roleColumn;
    @FXML private TableColumn<UserDTO, Double> balanceColumn;
    @FXML private TableColumn<UserDTO, String> banColumn;
    @FXML private TableColumn<UserDTO, String> actionsColumn;

    @FXML private TextField banDurationValueField;
    @FXML private ChoiceBox<String> banDurationUnitChoice;
    @FXML private TextField userIdSearchField;


    private boolean bannedOnly = false;
    private boolean initialized = false;
    private long loadVersion = 0;
    private List<UserDTO> loadedUsers = new ArrayList<>();

    /**
     * Called by FXMLLoader (AdminMenuController) to decide whether to show only banned users.
     */
    public void setBannedOnly(boolean bannedOnly) {
        boolean changed = this.bannedOnly != bannedOnly;
        this.bannedOnly = bannedOnly;
        if (initialized && changed) {
            renderUsers(applyLocalFilters(loadedUsers));
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!client.isConnected()) {
            client.connect();
        }

        if (!ClientSession.isAdmin()) {
            AlertUtils.showError("Không có quyền", "Chỉ Admin mới được vào trang này.");
            return;
        }

        setupColumns();

        if (backButton != null) {
            backButton.setOnAction(this::back);
        }

        if (userIdSearchField != null) {
            userIdSearchField.textProperty().addListener((obs, oldV, newV) -> applyIdSearchFilter(newV));
        }

        initialized = true;
        loadUsers(this.bannedOnly);
    }

    private void setupColumns() {
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        if (usernameColumn != null) {
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        }
        if (emailColumn != null) {
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        }
        if (roleColumn != null) {
            roleColumn.setCellValueFactory(cell -> {
                Role r = cell.getValue() == null ? null : cell.getValue().getRole();
                String text = r == null ? "" : r.name();
                return new javafx.beans.property.SimpleStringProperty(text);
            });
        }
        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        }
        if (banColumn != null) {
            banColumn.setCellValueFactory(cell -> {
                UserDTO u = cell.getValue();
                if (u == null) return new javafx.beans.property.SimpleStringProperty("");
                boolean isBanned = u.getBanEndTime() > System.currentTimeMillis();
                return new javafx.beans.property.SimpleStringProperty(isBanned ? "BAN" : "OK");
            });
        }

        if (actionsColumn != null && usersTableView != null) {
            actionsColumn.setCellFactory(col -> new TableCell<>() {

                private final Hyperlink banLink = new Hyperlink("BAN");
                private final Hyperlink unbanLink = new Hyperlink("UNBAN");
                private final javafx.scene.layout.HBox root = new javafx.scene.layout.HBox(8, banLink, unbanLink);

                {
                    banLink.setOnAction(e -> {
                        UserDTO u = getTableView().getItems().get(getIndex());
                        if (u == null) return;
                        applyBan(u.getId());
                    });

                    unbanLink.setOnAction(e -> {
                        UserDTO u = getTableView().getItems().get(getIndex());
                        if (u == null) return;
                        removeBan(u.getId());
                    });

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }

                    UserDTO u = getTableView().getItems().get(getIndex());
                    if (u == null) {
                        setGraphic(null);
                        return;
                    }

                    boolean isBanned = u.getBanEndTime() > System.currentTimeMillis();
                    banLink.setVisible(!isBanned);
                    banLink.setManaged(!isBanned);
                    unbanLink.setVisible(isBanned);
                    unbanLink.setManaged(isBanned);

                    setGraphic(root);
                }
            });
        }

        if (usersTableView != null) {
            usersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }

    private void loadUsers(boolean bannedOnly) {
        long requestVersion = ++loadVersion;
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                List<UserDTO> users = adminClientService.getAllUsers();
                if (users == null) {
                    return List.of();
                }
                return users;
            }
        };

        task.setOnSucceeded(e -> {
            if (requestVersion != loadVersion || usersTableView == null) {
                return;
            }
            List<UserDTO> users = task.getValue();
            if (users == null) users = List.of();
            loadedUsers = new ArrayList<>(users);
            renderUsers(applyLocalFilters(loadedUsers));
        });

        task.setOnFailed(e -> {
            if (requestVersion != loadVersion) {
                return;
            }
            Throwable ex = task.getException();
            String message = ex instanceof ClientServiceException ? ex.getMessage() : "Không thể tải danh sách người dùng.";
            AlertUtils.showError("Lỗi", message);
        });

        Thread t = new Thread(task, "admin-users-loader");
        t.setDaemon(true);
        t.start();
    }

    private void applyBan(int userId) {
        long durationMillis = parseBanDurationMillis();

        Task<ApplyBanResponse> task = new Task<>() {
            @Override
            protected ApplyBanResponse call() throws Exception {
                return adminClientService.applyBan(userId, durationMillis);
            }
        };

        task.setOnSucceeded(e -> {
            ApplyBanResponse resp = task.getValue();
            String msg = resp == null ? "Đã gửi yêu cầu." : resp.getMessage();
            AlertUtils.showInfo("Ban", msg);
            loadUsers(bannedOnly);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String message = ex == null ? "Không rõ lỗi" : ex.getMessage();
            AlertUtils.showError("Ban thất bại", message);
        });

        Thread t = new Thread(task, "admin-apply-ban");
        t.setDaemon(true);
        t.start();
    }

    private void removeBan(int userId) {
        Task<RemoveBanResponse> task = new Task<>() {
            @Override
            protected RemoveBanResponse call() throws Exception {
                return adminClientService.removeBan(userId);
            }
        };

        task.setOnSucceeded(e -> {
            RemoveBanResponse resp = task.getValue();
            String msg = resp == null ? "Đã gửi yêu cầu." : resp.getMessage();
            AlertUtils.showInfo("Unban", msg);
            loadUsers(bannedOnly);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String message = ex == null ? "Không rõ lỗi" : ex.getMessage();
            AlertUtils.showError("Unban thất bại", message);
        });

        Thread t = new Thread(task, "admin-remove-ban");
        t.setDaemon(true);
        t.start();
    }

    private void applyIdSearchFilter(String raw) {
        renderUsers(applyLocalFilters(loadedUsers));
    }

    private List<UserDTO> applyLocalFilters(List<UserDTO> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        String text = userIdSearchField == null ? "" : userIdSearchField.getText();
        text = text == null ? "" : text.trim();

        Integer searchId = null;
        if (!text.isEmpty()) {
            try {
                searchId = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return List.of();
            }
        }

        long now = System.currentTimeMillis();
        Integer finalSearchId = searchId;
        return source.stream()
                .filter(u -> u != null)
                .filter(u -> !bannedOnly || u.getBanEndTime() > now)
                .filter(u -> finalSearchId == null || u.getId() == finalSearchId)
                .toList();
    }

    private void renderUsers(List<UserDTO> users) {
        if (usersTableView == null) {
            return;
        }
        List<UserDTO> safeUsers = users == null ? List.of() : users;
        usersTableView.setItems(FXCollections.observableArrayList(safeUsers));
    }

    private long parseBanDurationMillis() {

        long defaultDuration = 7L * 24 * 60 * 60 * 1000;

        try {
            String rawVal = banDurationValueField == null ? null : banDurationValueField.getText();
            String rawUnit = banDurationUnitChoice == null ? null : banDurationUnitChoice.getValue();

            rawVal = rawVal == null ? "" : rawVal.trim();
            if (rawVal.isEmpty()) return defaultDuration;

            long value = Long.parseLong(rawVal);
            if (value <= 0) return defaultDuration;

            if (rawUnit == null) return defaultDuration;

            return switch (rawUnit) {
                case "Phút" -> value * 60_000L;
                case "Giờ" -> value * 3_600_000L;
                case "Ngày" -> value * 86_400_000L;
                default -> defaultDuration;
            };
        } catch (Exception ignored) {
            return defaultDuration;
        }
    }

    @FXML
    public void back(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/AdminScreen.fxml");
        } catch (IOException ex) {
            AlertUtils.showError("Lỗi điều hướng", ex.getMessage());
        }
    }
}

