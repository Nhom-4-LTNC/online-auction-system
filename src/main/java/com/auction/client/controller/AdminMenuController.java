package com.auction.client.controller;

import java.io.IOException;

import com.auction.client.network.Client;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.admin.ApplyBanRequest;
import com.auction.shared.protocol.admin.ApplyBanResponse;
import com.auction.shared.protocol.admin.GetAllUsersResponse;
import com.auction.shared.protocol.admin.RemoveBanRequest;
import com.auction.shared.protocol.admin.RemoveBanResponse;
import com.auction.shared.util.SceneUtils;
import com.auction.shared.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class AdminMenuController {

    private final Client client = Client.getInstance();

    @FXML private ListView<UserDTO> usersListView;
    @FXML private TextField targetUserIdTextField;
    @FXML private TextField durationMillisTextField;

    @FXML private Button banButton;
    @FXML private Button unbanButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        if (!client.isConnected()) {
            client.connect();
        }

        usersListView.setItems(FXCollections.observableArrayList());
        usersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String role = item.getRole() == null ? "" : item.getRole().name();

                long now = System.currentTimeMillis();
                boolean banned = item.getBanEndTime() > now;
                String banState = banned ? ("BANNED (ends in " + (item.getBanEndTime() - now) + " ms)") : "ACTIVE";

                setText(item.getId() + " | " + item.getUsername() + " | " + item.getEmail() + " | " + role + " | " + banState);
            }
        });

        // Listen server responses (admin actions)
        client.setOnMessageReceived(message -> {
            if (!(message instanceof Response<?> response)) return;

            if (response.getAction() == ActionType.GET_ALL_USERS) {
                if (!response.isSuccess()) {
                    showAlert("Lỗi", response.getErrorMessage());
                    return;
                }
                Object payload = response.getPayload();
                if (!(payload instanceof GetAllUsersResponse resp)) return;

                Platform.runLater(() -> {
                    if (resp.getUsers() == null) {
                        usersListView.setItems(FXCollections.observableArrayList());
                    } else {
                        usersListView.setItems(FXCollections.observableArrayList(resp.getUsers()));
                    }
                });
            }

            if (response.getAction() == ActionType.APPLY_BAN) {
                if (!response.isSuccess()) {
                    showAlert("Ban thất bại", response.getErrorMessage());
                    return;
                }
                Object payload = response.getPayload();
                if (payload instanceof ApplyBanResponse r) {
                    Platform.runLater(() -> showAlert("Thành công", r.getMessage()));
                }
                refresh(null);
            }

            if (response.getAction() == ActionType.REMOVE_BAN) {
                if (!response.isSuccess()) {
                    showAlert("Unban thất bại", response.getErrorMessage());
                    return;
                }
                Object payload = response.getPayload();
                if (payload instanceof RemoveBanResponse r) {
                    Platform.runLater(() -> showAlert("Thành công", r.getMessage()));
                }
                refresh(null);
            }
        });

        // guard: only ADMIN should access
        var cur = SessionManager.getInstance().getCurrentUser();
        if (cur == null || cur.getRole() != Role.ADMIN) {
            Platform.runLater(() -> showAlert("Không có quyền", "Chỉ Admin mới được vào trang này."));
        }

        // initial load
        refresh(null);
    }

    @FXML
    public void refresh(ActionEvent event) {
        client.sendMessage(new Request<>(ActionType.GET_ALL_USERS, null));
    }

    @FXML
    public void applyBan(ActionEvent event) {
        try {
            int targetUserId = Integer.parseInt(targetUserIdTextField.getText().trim());
            long durationMillis = Long.parseLong(durationMillisTextField.getText().trim());

            if (durationMillis <= 0) {
                showAlert("Lỗi", "durationMillis phải > 0");
                return;
            }

            client.sendMessage(new Request<>(ActionType.APPLY_BAN, new ApplyBanRequest(targetUserId, durationMillis)));
        } catch (Exception e) {
            showAlert("Lỗi", "Input không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    public void removeBan(ActionEvent event) {
        try {
            int targetUserId = Integer.parseInt(targetUserIdTextField.getText().trim());
            client.sendMessage(new Request<>(ActionType.REMOVE_BAN, new RemoveBanRequest(targetUserId)));
        } catch (Exception e) {
            showAlert("Lỗi", "Input không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        // Quay lại Home và logout cả client-side lẫn server-side để tránh tái dùng socket/session.
        // Nếu chỉ logout client-side thì server vẫn giữ currentUser trong ClientHandler.
        try {
            client.sendMessage(new Request<>(ActionType.LOGOUT, null));
        } catch (Exception ignored) {
            // best-effort
        }

        SessionManager.getInstance().logout();
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

