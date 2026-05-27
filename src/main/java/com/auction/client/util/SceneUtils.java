package com.auction.client.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class SceneUtils {

    private SceneUtils() {
    }

    public static void switchScene(ActionEvent event, String fxmlPath) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        switchScene(stage, fxmlPath);
    }

    public static void switchScene(Stage stage, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(resolveFxml(fxmlPath));
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static <T> T switchSceneAndGetController(ActionEvent event, String fxmlPath) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        return switchSceneAndGetController(stage, fxmlPath);
    }

    public static <T> T switchSceneAndGetController(Stage stage, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(resolveFxml(fxmlPath));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
        return loader.getController();
    }

    private static URL resolveFxml(String fxmlPath) throws IOException {
        URL resource = SceneUtils.class.getResource(fxmlPath);
        if (resource != null) {
            return resource;
        }

        String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath : "/fxml/" + fxmlPath;
        resource = SceneUtils.class.getResource(normalizedPath);
        if (resource == null) {
            throw new IOException("FXML resource not found: " + fxmlPath);
        }
        return resource;
    }
}
