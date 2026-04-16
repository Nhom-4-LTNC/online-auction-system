package com.auction.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/controller/example/demo10/Home.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("Năm anh em siêu nhân");
        Image image = new Image(getClass().getResource("/controller/example/demo10/nhom4.png").toExternalForm());
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
        alert.setHeaderText("You're exitting");
        alert.setContentText("Do you want to save before exiting?: ");

        if(alert.showAndWait().get() == ButtonType.OK){
            System.out.println("You logout successful");
            stage.close();
        }
    }
}
