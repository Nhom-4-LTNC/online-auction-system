package com.auction.client.util;

import javafx.scene.control.Alert;

public final class AlertUtils {

    private AlertUtils() {
    }

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    public static void info(String title, String message) {
        showInfo(title, message);
    }

    public static void error(String title, String message) {
        showError(title, message);
    }

    public static void warning(String title, String message) {
        showWarning(title, message);
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
