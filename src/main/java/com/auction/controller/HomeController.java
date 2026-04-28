package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    @FXML
    private Button logoutButton, thamgiaBUtton;
    @FXML
    private AnchorPane scenePane;

    Stage stage;
    Scene scene;
    Parent root;

    public void ThamGia(ActionEvent event) throws IOException{
        root = new FXMLLoader().load(getClass().getResource("/controller/example/demo10/fxml/AuctionMenu.fxml"));
        scene = new Scene((root));
        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    public void logout(ActionEvent event) throws IOException{

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout");
        alert.setContentText("Do you want to save before exiting?: ");

        if(alert.showAndWait().get() == ButtonType.OK){

            System.out.println("You logout successful");
            root = new FXMLLoader().load(getClass().getResource("/controller/example/demo10/fxml/LogInScreen.fxml"));
            scene = new Scene((root));
            stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        }
    }

    @FXML
    Label nameLabel, passLabel;

    public void displayName(String email) {

        UserData.email = email;

        if (Check.checkName(email)) {
            nameLabel.setText("Hello: " + email);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText("Invalid character. Please re-enter!");

            if (alert.showAndWait().get() == ButtonType.OK) {
                stage = (Stage) scenePane.getScene().getWindow();
                System.out.println("Loi");
            }
        }
    }

    public void displayPass(String pass) {

        UserData.password = pass;

        if (Check.checkPass(pass)) {
            String hiddenPass = "*".repeat(UserData.password.length());
            passLabel.setText("Pass: " +hiddenPass);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText("Invalid password. Please re-enter!");

            if (alert.showAndWait().get() == ButtonType.OK) {
                stage = (Stage) scenePane.getScene().getWindow();
                System.out.println("Loi");
            }
        }
    }

    @FXML
    CheckBox checkBox;

    public void change(ActionEvent event) throws IOException{
        if(checkBox.isSelected()){
            passLabel.setText("Pass: " +UserData.password);
        }
        else{
            String hiddenPass = "*".repeat(UserData.password.length());
            passLabel.setText("Pass: " + hiddenPass);
        }
    }
}
