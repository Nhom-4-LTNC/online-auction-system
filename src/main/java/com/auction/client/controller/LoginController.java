package com.auction.client.controller;

import com.auction.client.service.AuthClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.protocol.auth.AuthResponse;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final AuthClientService authClientService = new AuthClientService();

    @FXML private Button loginButton;
    @FXML private TextField emailTextField;
    @FXML private TextField visiblePasswordField;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    private volatile boolean loggingIn = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupPasswordVisibility();
    }

    @FXML
    public void login(ActionEvent event) {
        if (loggingIn) {
            return;
        }
        String email = emailTextField.getText().trim();
        String password = getPasswordFromFields();

        if (!validateInput(email, password)) {
            return;
        }

        loggingIn = true;
        setLoginSubmitting(true);

        Task<AuthResponse> task = new Task<>() {
            @Override
            protected AuthResponse call() {
                return authClientService.login(email, password);
            }
        };

        task.setOnSucceeded(workerEvent -> {
            try {
                handleLoginSuccess(task.getValue());
            } catch (IOException e) {
                loggingIn = false;
                setLoginSubmitting(false);
                AlertUtils.showError("Lỗi điều hướng", "Không thể mở màn hình tiếp theo.");
            }
        });

        task.setOnFailed(workerEvent -> {
            loggingIn = false;
            setLoginSubmitting(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể đăng nhập.";
            AlertUtils.showError("Đăng nhập thất bại", message);
        });

        task.setOnCancelled(workerEvent -> {
            loggingIn = false;
            setLoginSubmitting(false);
        });

        Thread thread = new Thread(task, "login-submit");
        thread.setDaemon(true);
        thread.start();
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
        if (loggingIn) {
            return;
        }
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

    private void handleLoginSuccess(AuthResponse authResponse) throws IOException {
        if (authResponse == null) {
            loggingIn = false;
            setLoginSubmitting(false);
            AlertUtils.showError("Đăng nhập thất bại", "Không nhận được phản hồi đăng nhập.");
            return;
        }

        UserDTO user = authResponse.getUser();

        if (user == null) {
            loggingIn = false;
            setLoginSubmitting(false);
            AlertUtils.showError("Đăng nhập thất bại", authResponse.getMessage());
            return;
        }

        ClientSession.setCurrentUser(user);
        navigateToHome(user);
    }

    private String getPasswordFromFields() {
        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : hiddenPasswordField.getText();
    }

    private boolean validateInput(String email, String password) {
        if (email == null || email.isBlank()) {
            AlertUtils.showError("Dữ liệu không hợp lệ", "Vui lòng nhập email.");
            return false;
        }

        if (password == null || password.isBlank()) {
            AlertUtils.showError("Dữ liệu không hợp lệ", "Vui lòng nhập mật khẩu.");
            return false;
        }

        return true;
    }

    private void navigateToHome(UserDTO user) throws IOException {
        Stage stage = (Stage) emailTextField.getScene().getWindow();
        if (user != null && user.getRole() == Role.ADMIN) {
            SceneUtils.switchScene(stage, "/fxml/AdminScreen.fxml");
            stage.setMaximized(true);
            return;
        }

        AuctionMenuController controller =
                SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionMenu.fxml");
        if (controller != null) {
            controller.setCurrentUser(user);
        }
        stage.setMaximized(true);
    }

    private void setLoginSubmitting(boolean submitting) {
        loginButton.setDisable(submitting);
        emailTextField.setDisable(submitting);
        visiblePasswordField.setDisable(submitting);
        hiddenPasswordField.setDisable(submitting);
        showPasswordCheckBox.setDisable(submitting);
        loginButton.setText(submitting ? "Đang xử lý..." : "Login");
    }
}
