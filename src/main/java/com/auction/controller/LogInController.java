package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LogInController implements Initializable {
    @FXML
    TextField nameTextField;
    @FXML
    TextField visiblePassword;
    @FXML
    PasswordField hiddenPassword;
    @FXML
    Button HomeButton;

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void login(ActionEvent event) throws IOException {

        String username = nameTextField.getText();
        String pass = hiddenPassword.getText();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("Scene2.fxml"));
        root = loader.load();

        MenuController scene2Controller = loader.getController();
        scene2Controller.displayName(username);
        scene2Controller.displayPass(pass);

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
        visiblePassword.textProperty().bindBidirectional(hiddenPassword.textProperty());
        visiblePassword.setVisible(false);
        hiddenPassword.setVisible(true);
    }

    public void BackToHome(ActionEvent event) throws IOException{
        root = new FXMLLoader().load(getClass().getResource("Home.fxml"));
        scene = new Scene((root));
        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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


}
