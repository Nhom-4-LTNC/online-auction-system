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
import com.auction.controller.MenuController;
import com.auction.controller.UserData;

import java.io.IOException;

public class SceneController  {
    Stage stage;
    Scene scene;
    Parent root;

    @FXML
    private Button BackButton;
    @FXML
    private AnchorPane scenePane;

    public void switchToScene2(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Leave");
        alert.setHeaderText("You're about to leave");
        alert.setContentText("Do you want to leave?");

        if(alert.showAndWait().get() == ButtonType.OK) {

            System.out.println("You have left");
            FXMLLoader loader =  new FXMLLoader(getClass().getResource("Scene2.fxml"));
            root = loader.load();

            MenuController menuController = loader.getController();
            menuController.displayName(UserData.username);
            menuController.displayPass(UserData.password);

            scene = new Scene((root));
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        }
    }
}
