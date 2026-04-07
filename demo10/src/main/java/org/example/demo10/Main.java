package org.example.demo10;

import javafx.application.Application;
import javafx.event.ActionEvent;
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
        Parent root = FXMLLoader.load(getClass().getResource("Scene1.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("Đấu giá của Bin Dat");
        Image image = new Image(getClass().getResource("nhom4.png").toExternalForm());
        stage.getIcons().add(image);
        stage.show();

        stage.setOnCloseRequest(event-> {
            event.consume();
            logout(stage);
            });
    }

    public void logout(Stage stage){

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout");
        alert.setContentText("Do you want to save before exiting?: ");

        if(alert.showAndWait().get() == ButtonType.OK){
            System.out.println("You logout successful");
            stage.close();
        }
    }
}
