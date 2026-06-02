package com.auction.client.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginScreen.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Online Auction System");

        var icon = getClass().getResource("/picture/nhom4.png");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }

        stage.setOnCloseRequest(event -> {
            event.consume();
            confirmExit(stage);
        });
        stage.show();
    }

    private void confirmExit(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("Thoat ung dung");
        alert.setContentText("Ban co chac muon thoat khong?");

        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            stage.close();
        }
    }
}
