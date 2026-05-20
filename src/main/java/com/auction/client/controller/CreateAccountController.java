package com.auction.client.controller;

import com.auction.client.Client;
import com.auction.dto.UserDTO;
import com.auction.protocol.ActionType;
import com.auction.protocol.Request;
import com.auction.protocol.Response;
import com.auction.protocol.auth.AuthResponse;
import com.auction.protocol.auth.RegisterRequest;
import com.auction.util.Check;
import com.auction.util.SceneUtils;
import com.auction.util.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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

    private final Client client = Client.getInstance();

    @FXML private Button Create;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client.connect();

        registerServerMessageHandler();
        registerPasswordValidationListener();
    }

    @FXML
    public void Create(ActionEvent event) {
        String email = emailTextField.getText().trim();
        String password = passTextField.getText();
        String username = nameTextField.getText().trim();

        if (!validateInput(username, email, password)) {
            return;
        }

        if (!client.isConnected()) {
            showAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        RegisterRequest registerRequest = new RegisterRequest(
                username,
                password,
                email
        );

        Request<RegisterRequest> request = new Request<>(
                ActionType.REGISTER,
                registerRequest
        );

        client.sendMessage(request);
    }

    @FXML
    public void backToLogin(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/login.fxml");
    }

    private void registerServerMessageHandler() {
        client.setOnMessageReceived(message -> {
            if (!(message instanceof Response<?> response)) {
                return;
            }

            if (response.getAction() != ActionType.REGISTER) {
                return;
            }

            Platform.runLater(() -> handleRegisterResponse(response));
        });
    }

    private void handleRegisterResponse(Response<?> response) {
        if (!response.isSuccess()) {
            showAlert("Đăng ký thất bại", response.getErrorMessage());
            return;
        }

        Object payload = response.getPayload();

        if (!(payload instanceof AuthResponse authResponse)) {
            showAlert("Lỗi", "Phản hồi đăng ký không đúng định dạng.");
            return;
        }

        UserDTO newUser = authResponse.getUser();

        if (newUser == null) {
            showAlert("Lỗi", authResponse.getMessage());
            return;
        }

        SessionManager.getInstance().setCurrentUser(newUser);

        try {
            navigateToHome(newUser);
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể chuyển màn hình: " + e.getMessage());
        }
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
            showAlert("Lỗi nhập liệu", "Vui lòng nhập tên người dùng!");
            return false;
        }

        if (email.isEmpty()) {
            showAlert("Lỗi nhập liệu", "Vui lòng nhập email!");
            return false;
        }

        if (!Check.checkPass(password)) {
            showAlert("Mật khẩu không hợp lệ", "Mật khẩu chưa đáp ứng đủ yêu cầu bảo mật!");
            return false;
        }

        return true;
    }

    private void navigateToHome(UserDTO user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) Create.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();

        HomeController homeController = loader.getController();
        homeController.displayName(user.getUsername());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setPwdReqImage(ImageView imgView, boolean valid) {
        imgView.setImage(valid ? validIcon : invalidIcon);
    }
}