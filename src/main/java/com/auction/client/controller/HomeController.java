package com.auction.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class HomeController implements Initializable {

    @FXML private AnchorPane scenePane;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UserDTO currentUser = ClientSession.getCurrentUser();
        if (currentUser == null) {
            renderGuestState();
            Platform.runLater(this::redirectToLogin);
            return;
        }

        renderCurrentUser(currentUser);
    }

    @FXML
    public void joinBid(ActionEvent event) throws IOException {
        handleBrowseAuctions(event);
    }

    @FXML
    public void createItem(ActionEvent event) throws IOException {
        handleCreateAuction(event);
    }

    @FXML
    public void goToFinance(ActionEvent event) throws IOException {
        handleWallet(event);
    }

    @FXML
    public void logout(ActionEvent event) throws IOException {
        handleLogout(event);
    }

    @FXML
    public void handleBrowseAuctions(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
    }

    @FXML
    public void handleCreateAuction(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/ItemMenu.fxml");
    }

    @FXML
    public void handleWallet(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/FinanceMenu.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        ClientSession.clear();
        SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");
    }

    public void displayName(String username) {
        UserDTO currentUser = ClientSession.getCurrentUser();
        if (currentUser != null) {
            renderCurrentUser(currentUser);
            return;
        }

        nameLabel.setText("Xin chào: " + safeText(username));
        if (roleLabel != null) {
            roleLabel.setText("Vai trò: N/A");
        }
        if (balanceLabel != null) {
            balanceLabel.setText("Số dư: N/A");
        }
    }

    private void renderCurrentUser(UserDTO user) {
        nameLabel.setText("Xin chào: " + safeText(user.getUsername()));
        if (roleLabel != null) {
            roleLabel.setText("Vai trò: " + (user.getRole() == null ? "N/A" : user.getRole().name()));
        }
        if (balanceLabel != null) {
            Double balance = ClientSession.getBalance();
            balanceLabel.setText("Số dư: " + (balance == null ? "N/A" : FormatUtils.currency(balance)));
        }
    }

    private void renderGuestState() {
        nameLabel.setText("Xin chào: Guest");
        if (roleLabel != null) {
            roleLabel.setText("Vai trò: N/A");
        }
        if (balanceLabel != null) {
            balanceLabel.setText("Số dư: N/A");
        }
    }

    private void redirectToLogin() {
        AlertUtils.showWarning("Phiên đăng nhập hết hạn", "Vui lòng đăng nhập lại.");
        try {
            Stage stage = (Stage) scenePane.getScene().getWindow();
            SceneUtils.switchScene(stage, "/fxml/LoginScreen.fxml");
        } catch (IOException e) {
            AlertUtils.showError("Lỗi điều hướng", "Không thể mở màn hình đăng nhập.");
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
