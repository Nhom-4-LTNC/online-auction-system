package com.auction.client.controller;

import com.auction.model.user.User;
import com.auction.client.Client;
import com.auction.protocol.ActionType;
import com.auction.protocol.auth.AuthRequest;
import com.auction.protocol.auth.AuthResponse;
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

    private Stage stage;
    private Scene scene;
    private Parent root;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Đồng bộ hai ô nhập mật khẩu
        visiblePasswordField.textProperty().bindBidirectional(hiddenPasswordField.textProperty());
        visiblePasswordField.setVisible(false);
        hiddenPasswordField.setVisible(true);

        // Kết nối tới server ngay khi mở màn hình đăng nhập
        client.connect();
    }

    @FXML
    public void login(ActionEvent event) {
        String email    = emailTextField.getText().trim();
        String password = getPasswordFromFields();

        if (!validateInput(email, password)) return;

        if (!client.isConnected()) {
            showErrorAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        // Đăng ký xử lý phản hồi từ server
        client.setOnMessageReceived(response -> {
            if (response instanceof AuthResponse authResponse) {
                if (authResponse.getResponseType() == ActionType.LOGIN_SUCCESS) {
                    try {
                        // Server xác nhận hợp lệ → lấy thông tin user từ DB
                        User user = authResponse.getUser();
                        navigateToHome(event, user);
                    } catch (Exception e) {
                        showErrorAlert("Lỗi", e.getMessage());
                    }
                } else if (authResponse.getResponseType() == ActionType.LOGIN_FAILURE) {
                    showErrorAlert("Đăng nhập thất bại", "Email hoặc mật khẩu không đúng!");
                }
            }
        });
        // Gửi yêu cầu đăng nhập lên server
        client.sendMessage(new AuthRequest(email, password));
    }

    @FXML
    public void togglePasswordVisibility(ActionEvent event) {
        boolean show = showPasswordCheckBox.isSelected();
        visiblePasswordField.setVisible(show);
        hiddenPasswordField.setVisible(!show);
    }

    @FXML
    public void createAccount(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
    }

    // ----------------------------------------------------------------
    // HELPER
    // ----------------------------------------------------------------

    private String getPasswordFromFields() {
        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : hiddenPasswordField.getText();
    }

    private boolean validateInput(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập email!");
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập mật khẩu!");
            return false;
        }
        return true;
    }

    private void navigateToHome(ActionEvent event, User user) throws IOException {
        SessionManager.getInstance().setCurrentUser(user);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        root = loader.load();

        // Gắn scene vào stage TRƯỚC khi gọi displayName()
        // Nếu đổi thứ tự: scenePane.getScene() trả về null → NullPointerException
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        // Dùng username (không dùng email) vì Check.checkName() kiểm tra quy tắc username
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
