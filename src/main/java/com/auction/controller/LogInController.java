package com.auction.controller;

import com.auction.dao.SceneUtils;
import com.auction.network.ActionType;
import com.auction.network.Client;
import com.auction.network.NetworkMessage;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LogInController implements Initializable {
    @FXML private TextField nameTextField;
    @FXML private PasswordField hiddenPassword;
    @FXML private TextField visiblePassword;
    @FXML private CheckBox Show;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Đồng bộ ô ẩn/hiện mật khẩu
        hiddenPassword.textProperty().bindBidirectional(visiblePassword.textProperty());

        // --- QUAN TRỌNG: KẾT NỐI SERVER KHI BẬT APP ---
        Client.getInstance().connect();
    }

    @FXML
    public void change(ActionEvent event) {
        if (Show.isSelected()) {
            visiblePassword.setVisible(true);
            hiddenPassword.setVisible(false);
            return;
        }
        visiblePassword.setVisible(false);
        hiddenPassword.setVisible(true);
    }

    @FXML
    public void CreateAccount(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void login(ActionEvent event) {
        String email = nameTextField.getText();
        String pass = hiddenPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng nhập Email và Mật khẩu!");
            alert.show();
            return;
        }

        // --- 1. LẮNG NGHE PHẢN HỒI TỪ SERVER ---
        Client.getInstance().setListener(msg -> {
            Platform.runLater(() -> {
                switch (msg.getAction()) {
                    case LOGIN_SUCCESS -> {
                        System.out.println("Đăng nhập thành công!");

                        // Lấy thông tin User từ Server trả về (Payload)
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userInfo = (Map<String, Object>) msg.getPayload();

                        // Lưu thông tin người dùng hiện tại (Tùy anh thiết kế Session)
                        // UserSession.getInstance().setCurrentUser(userInfo);

                        // Chuyển sang màn hình Home
                        try {
                            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    case LOGIN_FAILED -> {
                        String errorMsg = (String) msg.getPayload();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText(errorMsg);
                        alert.show();
                    }
                    default -> System.out.println("Nhận được gói tin không liên quan đến Login.");
                }
            });
        });

        // --- 2. GÓI DỮ LIỆU VÀ GỬI YÊU CẦU ĐĂNG NHẬP ---
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", pass);

        NetworkMessage request = new NetworkMessage(ActionType.LOGIN, credentials);
        Client.getInstance().sendMessage(request);
    }
}