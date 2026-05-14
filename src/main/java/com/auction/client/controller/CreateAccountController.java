package com.auction.client.controller;

<<<<<<< HEAD
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

=======
import com.auction.client.Client;
>>>>>>> cc1e837 (refactor(controller): remove direct service call)
import com.auction.model.user.User;
import com.auction.protocol.ActionType;
import com.auction.protocol.AuthRequest;
import com.auction.protocol.AuthResponse;
import com.auction.util.Check;
import com.auction.util.SceneUtils;
import com.auction.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class CreateAccountController implements Initializable {

    private final Client client = Client.getInstance();

    @FXML Button   Create;
    @FXML TextField passTextField;
    @FXML TextField emailTextField;
    @FXML TextField nameTextField;

    @FXML ImageView pwdReqNumber, pwdReqUpper, pwdReqLower, pwdReqSpecialChar, pwdReqNoWhites;

    Image validIcon   = new Image(getClass().getResource("/picture/validIcon.png").toExternalForm());
    Image invalidIcon = new Image(getClass().getResource("/picture/invalidIcon.png").toExternalForm());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Kết nối tới server (idempotent — an toàn nếu đã kết nối từ màn hình đăng nhập)
        client.connect();

        // Cập nhật icon kiểm tra mật khẩu theo thời gian thực
        passTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            ArrayList<Boolean> pwdRequirements = Check.checkPassRequirements(newValue);
            setPwdReqImage(pwdReqNumber,      pwdRequirements.get(0));
            setPwdReqImage(pwdReqLower,       pwdRequirements.get(1));
            setPwdReqImage(pwdReqUpper,       pwdRequirements.get(2));
            setPwdReqImage(pwdReqSpecialChar, pwdRequirements.get(3));
            setPwdReqImage(pwdReqNoWhites,    pwdRequirements.get(4));
        });
    }

    @FXML
    public void Create(ActionEvent event) {
        String email    = emailTextField.getText().trim();
        String pass     = passTextField.getText();
        String username = nameTextField.getText().trim();

        if (!validateInput(username, email, pass)) return;

        if (!client.isConnected()) {
            showAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        // Đăng ký xử lý phản hồi từ server
        client.setOnMessageReceived(response -> {
            if (response instanceof AuthResponse authResponse) {
                if (authResponse.getResponseType() == ActionType.REGISTER_SUCCESS) {
                    User newUser = authResponse.getUser();
                    SessionManager.getInstance().setCurrentUser(newUser);
                    try {
                        navigateToHome(event, newUser);
                    } catch (IOException e) {
                        showAlert("Lỗi", "Không thể chuyển màn hình: " + e.getMessage());
                    }
                } else if (authResponse.getResponseType() == ActionType.REGISTER_FAILURE) {
                    showAlert("Đăng ký thất bại", authResponse.getMessage());
                }
            }
        });

        // Gửi yêu cầu đăng ký lên server
        client.sendMessage(new AuthRequest(username, email, pass));
    }

    @FXML
    public void backToLogin(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/login.fxml");
    }

    // ----------------------------------------------------------------
    // HELPER
    // ----------------------------------------------------------------

    private boolean validateInput(String username, String email, String pass) {
        if (username.isEmpty()) {
            showAlert("Lỗi nhập liệu", "Vui lòng nhập tên người dùng!");
            return false;
        }
        if (email.isEmpty()) {
            showAlert("Lỗi nhập liệu", "Vui lòng nhập email!");
            return false;
        }
        if (!Check.checkPass(pass)) {
            showAlert("Mật khẩu không hợp lệ", "Mật khẩu chưa đáp ứng đủ yêu cầu bảo mật!");
            return false;
        }
        return true;
    }

    private void navigateToHome(ActionEvent event, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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
