package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    private Parent root;
    private Scene scene;
    private Stage stage;
    @FXML
    Button signInButton;
    @FXML
    Button signOutButton;
    @FXML
    AnchorPane scenePane;

    public void SignIn(ActionEvent event) throws IOException{
        root = new FXMLLoader().load(getClass().getResource("/controller/example/demo10/Scene1.fxml"));
        scene = new Scene((root));
        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    public void SignOut(ActionEvent event){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("SignOut");
        alert.setHeaderText("You're about to SignOut");
        alert.setContentText("Do you want to save before exiting?: ");

        if(alert.showAndWait().get() == ButtonType.OK){
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }

    }
}
