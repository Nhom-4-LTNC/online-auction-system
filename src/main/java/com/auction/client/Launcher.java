package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    public static void main(String[] args) {
        Application.launch(Launcher.class, args);
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
        alert.setHeaderText("Thoát ứng dụng");
        alert.setContentText("Bạn có chắc muốn thoát không?");

        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            stage.close();
        }
    }
}
