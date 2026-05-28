package com.auction.client.controller;

import com.auction.client.service.AuthClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.protocol.auth.AuthResponse;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final AuthClientService authClientService = new AuthClientService();

    @FXML private TextField emailTextField;
    @FXML private TextField visiblePasswordField;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private CheckBox showPasswordCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupPasswordVisibility();
    }

    @FXML
    public void login(ActionEvent event) {
        String email = emailTextField.getText().trim();
        String password = getPasswordFromFields();

        if (!validateInput(email, password)) {
            return;
        }

        try {
            AuthResponse authResponse = authClientService.login(email, password);
            handleLoginSuccess(authResponse);
        } catch (ClientServiceException e) {
            AlertUtils.showError("Login failed", e.getMessage());
        } catch (IOException e) {
            AlertUtils.showError("Navigation error", "Cannot open screen: " + e.getMessage());
        }
    }

    @FXML
    public void togglePasswordVisibility(ActionEvent event) {
        boolean show = showPasswordCheckBox.isSelected();

        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);

        hiddenPasswordField.setVisible(!show);
        hiddenPasswordField.setManaged(!show);
    }

    @FXML
    public void createAccount(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
    }

    private void setupPasswordVisibility() {
        visiblePasswordField.textProperty()
                .bindBidirectional(hiddenPasswordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        hiddenPasswordField.setVisible(true);
        hiddenPasswordField.setManaged(true);
    }

    private void handleLoginSuccess(AuthResponse authResponse) throws IOException {
        UserDTO user = authResponse.getUser();

        if (user == null) {
            AlertUtils.showError("Login failed", authResponse.getMessage());
            return;
        }

        ClientSession.setCurrentUser(user);
        navigateToHome(user);
    }

    private String getPasswordFromFields() {
        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : hiddenPasswordField.getText();
    }

    private boolean validateInput(String email, String password) {
        if (email == null || email.isBlank()) {
            AlertUtils.showError("Input error", "Please enter email.");
            return false;
        }

        if (password == null || password.isBlank()) {
            AlertUtils.showError("Input error", "Please enter password.");
            return false;
        }

        return true;
    }

    private void navigateToHome(UserDTO user) throws IOException {
        String fxml = (user.getRole() != null && user.getRole() == Role.ADMIN)
                ? "/fxml/AdminScreen.fxml"
                : "/fxml/HomeScreen.fxml";

        Stage stage = (Stage) emailTextField.getScene().getWindow();
        Object controller = SceneUtils.switchSceneAndGetController(stage, fxml);

        if (controller instanceof HomeController homeController) {
            homeController.displayName(user.getUsername());
        }
    }
}
