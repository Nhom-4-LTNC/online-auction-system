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

public class HomeMenuController extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginScreen.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("Năm anh em siêu nhân");
        Image image = new Image(getClass().getResource("/picture/nhom4.png").toExternalForm());
        stage.getIcons().add(image);
        stage.show();

        stage.setOnCloseRequest(event-> {
            event.consume();
            logout(stage);
            });
    }

    public void logout(Stage stage){

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("Thoát ứng dụng");
        alert.setContentText("Bạn có chắc muốn thoát không?");

        if(alert.showAndWait().get() == ButtonType.OK){
            stage.close();
        }
    }
}
