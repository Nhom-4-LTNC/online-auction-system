package com.auction.client.controller;

import java.io.IOException;

import com.auction.service.UserService;
import com.auction.util.SceneUtils;
import com.auction.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class FinanceMenuController {

    @FXML private TextField incrementBalanceTextField;
    @FXML private TextField setBalanceTextField;
    @FXML private Button incrementBalanceButton;
    @FXML private Button setBalanceButton;
    @FXML private Button backButton;

    private final UserService userService = UserService.getInstance();

    @FXML
    public void incrementBalance(ActionEvent event) {
        try {
            String input = incrementBalanceTextField.getText().trim();
            if (input.isEmpty()) {
                showAlert("Error", "Please enter an amount!");
                return;
            }

            double amount = Double.parseDouble(input);
            var currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                showAlert("Error", "Please login first!");
                return;
            }

            // Get bidder profile and increment balance
            var bidderProfile = currentUser.getBidderProfile();
            double newBalance = bidderProfile.getBalance() + amount;

            // Validate balance is not negative
            if (newBalance < 0) {
                showAlert("Error", "Balance cannot be negative!");
                return;
            }

            // Save updated balance to database
            userService.updateUserBalance(currentUser, newBalance);

            showInfo("Success", String.format("Balance updated! New balance: %.2f", newBalance));
            incrementBalanceTextField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number!");
        } catch (Exception e) {
            showAlert("Error", "Failed to update balance: " + e.getMessage());
        }
    }

    @FXML
    public void setBalance(ActionEvent event) {
        try {
            String input = setBalanceTextField.getText().trim();
            if (input.isEmpty()) {
                showAlert("Error", "Please enter an amount!");
                return;
            }

            double amount = Double.parseDouble(input);
            var currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                showAlert("Error", "Please login first!");
                return;
            }

            // Validate amount is not negative
            if (amount < 0) {
                showAlert("Error", "Balance cannot be negative!");
                return;
            }

            // Save updated balance to database
            userService.updateUserBalance(currentUser, amount);

            showInfo("Success", String.format("Balance set to: %.2f", amount));
            setBalanceTextField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number!");
        } catch (Exception e) {
            showAlert("Error", "Failed to set balance: " + e.getMessage());
        }
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

