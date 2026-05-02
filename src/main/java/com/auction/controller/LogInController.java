package com.auction.controller;

import com.auction.dao.UserData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        HashMap<String, String> map = UserData.users;
        String email = nameTextField.getText();
        String pass = hiddenPassword.getText();

        if(map.containsKey(email)){
            if (map.get(email).equals(pass)){
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
                root = loader.load();

                HomeController homeController = loader.getController();
                homeController.displayName(email);
                homeController.displayPass(pass);

                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }else{
                System.out.println("Nhập sai mật khẩu");
            }
        }else{
            System.out.println("Không tồn tại tài khoản");
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createAccount.fxml"));
        root = loader.load();
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

}
