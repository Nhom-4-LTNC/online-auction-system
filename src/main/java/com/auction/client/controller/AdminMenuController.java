package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.auction.client.network.Client;
import com.auction.client.service.AdminClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.admin.ApplyBanResponse;
import com.auction.shared.protocol.admin.RemoveBanResponse;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Refactor theo UI_REFACTOR_GUIDE: controller chỉ xử lý UI và gọi client-side service.
 */
public class AdminMenuController implements Initializable {

    private final Client client = Client.getInstance();
    private final AdminClientService adminClientService = new AdminClientService();

    @FXML private ListView<UserDTO> usersListView;
    @FXML private TextField targetUserIdTextField;
    @FXML private TextField durationMillisTextField;

    @FXML private Button banButton;
    @FXML private Button unbanButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        if (!ClientSession.isAdmin()) {
            AlertUtils.showError("Không có quyền", "Chỉ Admin mới được vào trang này.");
            return;
        }

        refresh(null);

    }

    @FXML
    public void refresh(ActionEvent event) {
        try {
            List<UserDTO> users = adminClientService.getAllUsers();
            usersListView.setItems(FXCollections.observableArrayList(users));
        } catch (ClientServiceException e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Không thể tải users: " + e.getMessage());
        }
    }

    @FXML
    public void applyBan(ActionEvent event) {
        try {
            int targetUserId = Integer.parseInt(targetUserIdTextField.getText().trim());
            long durationMillis = Long.parseLong(durationMillisTextField.getText().trim());

            if (durationMillis <= 0) {
                AlertUtils.showError("Lỗi", "durationMillis phải > 0");
                return;
            }

            ApplyBanResponse result = adminClientService.applyBan(targetUserId, durationMillis);
            AlertUtils.showInfo("Thành công", result.getMessage());
            refresh(null);
        } catch (ClientServiceException e) {
            AlertUtils.showError("Ban thất bại", e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Input không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    public void removeBan(ActionEvent event) {
        try {
            int targetUserId = Integer.parseInt(targetUserIdTextField.getText().trim());

            RemoveBanResponse result = adminClientService.removeBan(targetUserId);
            AlertUtils.showInfo("Thành công", result.getMessage());
            refresh(null);
        } catch (ClientServiceException e) {
            AlertUtils.showError("Unban thất bại", e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Input không hợp lệ: " + e.getMessage());
        }
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        try {
            // giữ nguyên behavior logout server-side hiện tại bằng cách clear client session
            ClientSession.clear();
        } catch (Exception ignored) {
        }

        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }
}

