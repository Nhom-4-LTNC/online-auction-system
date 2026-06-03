package com.auction.client.controller;

import com.auction.client.service.AuthClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.auth.AuthResponse;
import com.auction.client.util.Check;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class CreateAccountController implements Initializable {

    private final AuthClientService authClientService = new AuthClientService();

    @FXML private Button Create;
    @FXML private Button signInButton;
    @FXML private TextField passTextField;
    @FXML private TextField emailTextField;
    @FXML private TextField nameTextField;

    @FXML private ImageView pwdReqNumber;
    @FXML private ImageView pwdReqUpper;
    @FXML private ImageView pwdReqLower;
    @FXML private ImageView pwdReqSpecialChar;
    @FXML private ImageView pwdReqNoWhites;

    private final Image validIcon =
            new Image(getClass().getResource("/picture/validIcon.png").toExternalForm());

    private final Image invalidIcon =
            new Image(getClass().getResource("/picture/invalidIcon.png").toExternalForm());
    private volatile boolean registering = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        registerPasswordValidationListener();
    }

    @FXML
    public void Create(ActionEvent event) {
        if (registering) {
            return;
        }
        String email = emailTextField.getText().trim();
        String password = passTextField.getText();
        String username = nameTextField.getText().trim();

        if (!validateInput(username, email, password)) {
            return;
        }

        registering = true;
        setRegisterSubmitting(true);

        Task<AuthResponse> task = new Task<>() {
            @Override
            protected AuthResponse call() {
                return authClientService.register(username, email, password);
            }
        };

        task.setOnSucceeded(workerEvent -> {
            try {
                handleRegisterSuccess(task.getValue());
            } catch (IOException e) {
                registering = false;
                setRegisterSubmitting(false);
                AlertUtils.showError("Lỗi điều hướng", "Không thể mở màn hình tiếp theo.");
            }
        });

        task.setOnFailed(workerEvent -> {
            registering = false;
            setRegisterSubmitting(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể đăng ký.";
            AlertUtils.showError("Đăng ký thất bại", message);
        });

        task.setOnCancelled(workerEvent -> {
            registering = false;
            setRegisterSubmitting(false);
        });

        Thread thread = new Thread(task, "register-submit");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void goToSignIn(ActionEvent event) throws IOException {
        if (registering) {
            return;
        }
        SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");
    }

    private void handleRegisterSuccess(AuthResponse authResponse) throws IOException {
        if (authResponse == null) {
            registering = false;
            setRegisterSubmitting(false);
            AlertUtils.showError("Đăng ký thất bại", "Không nhận được phản hồi đăng ký.");
            return;
        }

        UserDTO newUser = authResponse.getUser();

        if (newUser == null) {
            registering = false;
            setRegisterSubmitting(false);
            AlertUtils.showError("Đăng ký thất bại", authResponse.getMessage());
            return;
        }

        ClientSession.setCurrentUser(newUser);
        navigateToHome(newUser);
    }

    private void registerPasswordValidationListener() {
        passTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            ArrayList<Boolean> pwdRequirements = Check.checkPassRequirements(newValue);

            setPwdReqImage(pwdReqNumber, pwdRequirements.get(0));
            setPwdReqImage(pwdReqLower, pwdRequirements.get(1));
            setPwdReqImage(pwdReqUpper, pwdRequirements.get(2));
            setPwdReqImage(pwdReqSpecialChar, pwdRequirements.get(3));
            setPwdReqImage(pwdReqNoWhites, pwdRequirements.get(4));
        });
    }

    private boolean validateInput(String username, String email, String password) {
        if (username.isEmpty()) {
            AlertUtils.showError("Dữ liệu không hợp lệ", "Vui lòng nhập username.");
            return false;
        }

        if (email.isEmpty()) {
            AlertUtils.showError("Dữ liệu không hợp lệ", "Vui lòng nhập email.");
            return false;
        }

        if (!Check.checkPass(password)) {
            AlertUtils.showError("Mật khẩu không hợp lệ", "Mật khẩu chưa đáp ứng yêu cầu bảo mật.");
            return false;
        }

        return true;
    }

    private void navigateToHome(UserDTO user) throws IOException {
        Stage stage = (Stage) Create.getScene().getWindow();
        AuctionMenuController controller =
                SceneUtils.switchSceneAndGetController(stage, "/fxml/AuctionMenu.fxml");
        if (controller != null) {
            controller.setCurrentUser(user);
        }
        stage.setMaximized(true);
    }

    private void setPwdReqImage(ImageView imgView, boolean valid) {
        imgView.setImage(valid ? validIcon : invalidIcon);
    }

    private void setRegisterSubmitting(boolean submitting) {
        Create.setDisable(submitting);
        signInButton.setDisable(submitting);
        nameTextField.setDisable(submitting);
        emailTextField.setDisable(submitting);
        passTextField.setDisable(submitting);
        Create.setText(submitting ? "Đang xử lý..." : "Create");
    }
}
