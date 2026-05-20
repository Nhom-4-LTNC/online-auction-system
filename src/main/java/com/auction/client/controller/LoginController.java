package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auth.AuthResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.util.SceneUtils;
import com.auction.shared.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final Client client = Client.getInstance();

    @FXML private TextField emailTextField;
    @FXML private TextField visiblePasswordField;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private CheckBox showPasswordCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupPasswordVisibility();
        client.connect();
        registerServerMessageHandler();
    }

    @FXML
    public void login(ActionEvent event) {
        String email = emailTextField.getText().trim();
        String password = getPasswordFromFields();

        if (!validateInput(email, password)) {
            return;
        }

        if (!client.isConnected()) {
            showErrorAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        sendLoginRequest(email, password);
    }

    @FXML
    public void togglePasswordVisibility(ActionEvent event) {
        boolean show = showPasswordCheckBox.isSelected();

        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);

        hiddenPasswordField.setVisible(!show);
        hiddenPasswordField.setManaged(!show);
    }

    @FXML
    public void createAccount(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
    }

    private void setupPasswordVisibility() {
        visiblePasswordField.textProperty()
                .bindBidirectional(hiddenPasswordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        hiddenPasswordField.setVisible(true);
        hiddenPasswordField.setManaged(true);
    }

    private void sendLoginRequest(String email, String password) {
        LoginRequest payload = new LoginRequest(email, password);

        Request<LoginRequest> request = new Request<>(
                ActionType.LOGIN,
                payload
        );

        client.sendMessage(request);
    }

    private void registerServerMessageHandler() {
        client.setOnMessageReceived(message -> {
            if (!(message instanceof Response<?> response)) {
                return;
            }

            if (response.getAction() != ActionType.LOGIN) {
                return;
            }

            Platform.runLater(() -> handleLoginResponse(response));
        });
    }

    private void handleLoginResponse(Response<?> response) {
        if (!response.isSuccess()) {
            showErrorAlert("Đăng nhập thất bại", response.getErrorMessage());
            return;
        }

        Object payload = response.getPayload();

        if (!(payload instanceof AuthResponse authResponse)) {
            showErrorAlert("Lỗi", "Phản hồi đăng nhập không đúng định dạng.");
            return;
        }

        UserDTO user = authResponse.getUser();

        if (user == null) {
            showErrorAlert("Đăng nhập thất bại", authResponse.getMessage());
            return;
        }

        SessionManager.getInstance().setCurrentUser(user);

        try {
            navigateToHome(user);
        } catch (IOException e) {
            showErrorAlert("Lỗi", "Không thể chuyển màn hình: " + e.getMessage());
        }
    }

    private String getPasswordFromFields() {
        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : hiddenPasswordField.getText();
    }

    private boolean validateInput(String email, String password) {
        if (email == null || email.isBlank()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập email!");
            return false;
        }

        if (password == null || password.isBlank()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập mật khẩu!");
            return false;
        }

        return true;
    }

    private void navigateToHome(UserDTO user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) emailTextField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

        HomeController homeController = loader.getController();
        homeController.displayName(user.getUsername());
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}