package com.auction.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.auction.model.user.User;
import com.auction.service.UserService;
import com.auction.util.Check;
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

    // Dùng singleton để cả ứng dụng dùng chung một UserService
    private final UserService userService = UserService.getInstance();

    @FXML Button Create;
    @FXML TextField passTextField;
    @FXML TextField emailTextField;
    @FXML TextField nameTextField;

    @FXML ImageView pwdReqNumber, pwdReqUpper, pwdReqLower, pwdReqSpecialChar, pwdReqNoWhites;

    Stage stage;
    Scene scene;
    Parent root;

    Image validIcon   = new Image(getClass().getResource("/picture/validIcon.png").toExternalForm());
    Image invalidIcon = new Image(getClass().getResource("/picture/invalidIcon.png").toExternalForm());

    private void setPwdReqImage(ImageView imgView, boolean valid) {
        imgView.setImage(valid ? validIcon : invalidIcon);
    }

    public void Create(ActionEvent event) throws IOException {
        String email    = emailTextField.getText().trim();
        String pass     = passTextField.getText();
        String username = nameTextField.getText().trim();

        // Kiểm tra mật khẩu đủ yêu cầu trước khi gửi lên service
        if (!Check.checkPass(pass)) {
            showAlert("Mật khẩu không hợp lệ", "Mật khẩu chưa đáp ứng đủ yêu cầu bảo mật!");
            return;
        }

        try {
            // Gọi UserService để đăng ký — service sẽ kiểm tra trùng email/username
            User newUser = userService.register(username, email, pass);

            // Lưu vào SessionManager để dùng ở màn hình tiếp theo
            SessionManager.getInstance().setCurrentUser(newUser);

            System.out.println("Tạo tài khoản thành công: " + newUser.getUsername());

            // Chuyển sang màn hình chính
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
            root = loader.load();

            HomeController homeController = loader.getController();
            homeController.displayName(newUser.getEmail());

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            showAlert("Đăng ký thất bại", e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        passTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            ArrayList<Boolean> pwdRequirements = Check.checkPassRequirements(passTextField.getText());
            setPwdReqImage(pwdReqNumber,      pwdRequirements.get(0));
            setPwdReqImage(pwdReqLower,       pwdRequirements.get(1));
            setPwdReqImage(pwdReqUpper,       pwdRequirements.get(2));
            setPwdReqImage(pwdReqSpecialChar, pwdRequirements.get(3));
            setPwdReqImage(pwdReqNoWhites,    pwdRequirements.get(4));
        });
    }
}
