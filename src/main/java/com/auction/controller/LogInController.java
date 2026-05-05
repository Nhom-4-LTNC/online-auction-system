package com.auction.controller;

import com.auction.dao.Check;
import com.auction.dao.SceneUtils;
import com.auction.dao.UserDAO;
import com.auction.model.user.User;
import com.auction.model.user.UserManager;
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
import java.util.HashMap;
import java.util.ResourceBundle;

public class LogInController implements Initializable {
    @FXML
    TextField nameTextField;
    @FXML
    TextField visiblePassword;
    @FXML
    PasswordField hiddenPassword;

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void login(ActionEvent event) throws IOException {

        String email = nameTextField.getText();
        String pass = hiddenPassword.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng nhập đầy đủ Email và Mật khẩu!");
            alert.show();
            return; // BLOCK
        }
        try {
            User user = UserManager.getInstance().getUserByEmail(email);
            if (user.getPwd().equals(pass)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
                root = loader.load();

                HomeController homeController = loader.getController();
                homeController.displayName(email);
                Check.checkPass(pass);

                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }else{
                System.out.println("Nhap sai mat khau: ");
            }
        } catch (Exception e) {
            System.out.println("Khong ton tai tai khoan");
        }
    }

    @FXML
    CheckBox Show;
    public void change(ActionEvent event){
        if(Show.isSelected()){
            visiblePassword.setVisible(true);
            hiddenPassword.setVisible(false);
        }
        else{
            visiblePassword.setVisible(false);
            hiddenPassword.setVisible(true);
        }
    }

    // HomeMenuController
    public void initialize(URL url, ResourceBundle resourceBundle) {
        visiblePassword.textProperty().bindBidirectional(hiddenPassword.textProperty());
        visiblePassword.setVisible(false);
        hiddenPassword.setVisible(true);
    }

    public void CreateAccount(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
    }

}
