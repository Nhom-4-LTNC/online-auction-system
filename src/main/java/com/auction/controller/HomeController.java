package com.auction.controller;

import java.io.IOException;

import com.auction.util.Check;
import com.auction.util.SceneUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class HomeController {
    @FXML
    private Button logoutButton, thamgiaBUtton;
    @FXML
    private AnchorPane scenePane;

    Stage stage;
    Scene scene;
    Parent root;

    public void joinBid(ActionEvent event) throws IOException{
        SceneUtils.switchScene(event, "AC.fxml");
    }

    public void createItem(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/ItemMenu.fxml");
    }

    public void logout(ActionEvent event) throws IOException{

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout");
        alert.setContentText("Do you really want to exit?: ");

        if(alert.showAndWait().get() == ButtonType.OK){

            System.out.println("You logout successful");
            SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");

        }
    }
    public void joinAuction(ActionEvent event) throws IOException{
        SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
    }

    // DISPLAY
    @FXML
    Label nameLabel, passLabel;

    public void displayName(String email) {

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
}
