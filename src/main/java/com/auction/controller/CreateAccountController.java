package com.auction.controller;


import com.auction.dao.Check;
import com.auction.model.user.User;
import com.auction.model.user.UserManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class CreateAccountController implements Initializable {
    @FXML
    Button Create, loginScreenButton;
    @FXML
    TextField passTextField;
    @FXML
    TextField emailTextField;
    @FXML
    TextField nameTextField;

    @FXML
    ImageView pwdReqNumber, pwdReqUpper, pwdReqLower, pwdReqSpecialChar, pwdReqNoWhites;

    Stage stage;
    Scene scene;
    Parent root;

    Image validIcon = new Image(getClass().getResource("/picture/validIcon.png").toExternalForm());
    Image invalidIcon = new Image(getClass().getResource("/picture/invalidIcon.png").toExternalForm());

    private void setPwdReqImage(ImageView imgView, boolean valid) {
        if (valid) {
            imgView.setImage(validIcon);
        } else {
            imgView.setImage(invalidIcon);
        }
    }

    public void goToLoginScreen(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LogInScreen.fxml"));
        root = loader.load();
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void Create(ActionEvent event) throws IOException {
        String email = emailTextField.getText();
        String pass = passTextField.getText();
        String username = nameTextField.getText();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        root = loader.load();

        HomeController homeController = loader.getController();
        homeController.displayName(email);
        boolean isPasswordCorrect = Check.checkPass(pass);

        if (isPasswordCorrect) {
            System.out.println("Tạo tài khoản thành công");

            User user = new User(username, pass, email);
            UserManager.getInstance().addUser(user);

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        passTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Fires whenever the password text field is changed
            // and changes requirement icons
            ArrayList<Boolean> pwdRequirements = Check.checkPassRequirements(passTextField.getText());

            setPwdReqImage(pwdReqNumber, pwdRequirements.get(0));
            setPwdReqImage(pwdReqLower, pwdRequirements.get(1));
            setPwdReqImage(pwdReqUpper, pwdRequirements.get(2));
            setPwdReqImage(pwdReqSpecialChar, pwdRequirements.get(3));
            setPwdReqImage(pwdReqNoWhites, pwdRequirements.get(4));
        });
    }
}
