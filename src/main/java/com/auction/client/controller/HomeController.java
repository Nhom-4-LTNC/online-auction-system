package com.auction.client.controller;

import java.io.IOException;
<<<<<<< Updated upstream


import com.auction.shared.util.Check;
import com.auction.shared.util.SceneUtils;
=======
import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;

import javafx.application.Platform;
>>>>>>> Stashed changes
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

<<<<<<< Updated upstream
import javax.swing.*;

public class HomeController {
=======
public class HomeController implements Initializable {

    @FXML private AnchorPane scenePane;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;

    private final com.auction.shared.protocol.ActionType ADD_BALANCE = com.auction.shared.protocol.ActionType.ADD_BALANCE;
    private java.util.function.Consumer<com.auction.shared.protocol.Response<?>> addBalanceListener;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1) Render initial state
        UserDTO currentUser = ClientSession.getCurrentUser();
        if (currentUser == null) {
            renderGuestState();
            Platform.runLater(this::redirectToLogin);
            return;
        }
        renderCurrentUser(currentUser);

        // 2) Subscribe to wallet updates so Home balance is always refreshed
        subscribeAddBalanceListener();
    }

    private void subscribeAddBalanceListener() {
        // avoid double-subscribe if initialize is called again
        if (addBalanceListener != null) return;

        addBalanceListener = response -> {
            if (response == null) return;
            if (!response.isSuccess()) return;

            // Ensure UI update on FX thread
            Platform.runLater(() -> {
                UserDTO user = ClientSession.getCurrentUser();
                if (user != null) {
                    renderCurrentUser(user);
                }
            });
        };

        // Ensure socket is connected before registering network listeners
        com.auction.client.network.Client client = com.auction.client.network.Client.getInstance();
        if (!client.isConnected()) {
            client.connect();
        }
        client.addEventListener(ADD_BALANCE, addBalanceListener);
    }



>>>>>>> Stashed changes
    @FXML
    private AnchorPane scenePane;

    Stage stage;
    Scene scene;
    Parent root;

    public void joinBid(ActionEvent event) throws IOException{
        SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
    }

    public void createItem(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/ItemMenu.fxml");
    }

    public void goToFinance(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/FinanceMenu.fxml");
    }

    public void logout(ActionEvent event) throws IOException{

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout");
        alert.setContentText("Do you really want to exit?: ");

        if(alert.showAndWait().get() == ButtonType.OK){

            System.out.println("You logout successful");
            SceneUtils.switchScene(event, "/fxml/LoginScreen.fxml");

        }
    }

    // DISPLAY
    @FXML
    Label nameLabel, passLabel;

    public void displayName(String email) {

        if (Check.checkName(email)) {
            nameLabel.setText("Hello: " + email);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText("Invalid character. Please re-enter!");

            if (alert.showAndWait().get() == ButtonType.OK) {
                stage = (Stage) scenePane.getScene().getWindow();
                System.out.println("Loi");
            }
        }
    }
}
