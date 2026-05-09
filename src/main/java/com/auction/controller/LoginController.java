package com.auction.controller;

import com.auction.model.user.User;
import com.auction.service.UserService;
import com.auction.util.SceneUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // Service layer
    private final UserService userService = new UserService();

    // UI Components
    @FXML
    private TextField emailTextField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private PasswordField hiddenPasswordField;
    @FXML
    private CheckBox showPasswordCheckBox;

    // Scene management
    private Stage stage;
    private Scene scene;
    private Parent root;

    /**
     * Handle login button click
     */
    @FXML
    public void login(ActionEvent event) throws IOException {
        String email = emailTextField.getText().trim();
        String password = getPasswordFromFields();

        // Input validation
        if (!validateInput(email, password)) {
            return;
        }

        try {
            // Business logic - delegate to service
            User user = userService.login(email, password);

            // UI logic - navigate to home
            navigateToHome(event, user);

        } catch (Exception e) {
            showErrorAlert("Đăng nhập thất bại", e.getMessage());
        }
    }

    /**
     * Handle password visibility toggle
     */
    @FXML
    public void togglePasswordVisibility(ActionEvent event) {
        boolean showPassword = showPasswordCheckBox.isSelected();

        visiblePasswordField.setVisible(showPassword);
        hiddenPasswordField.setVisible(!showPassword);

        // Sync password text between fields
        if (showPassword) {
            visiblePasswordField.setText(hiddenPasswordField.getText());
        } else {
            hiddenPasswordField.setText(visiblePasswordField.getText());
        }
    }

    /**
     * Handle create account button click
     */
    @FXML
    public void createAccount(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/createAccount.fxml");
    }

    /**
     * Initialize the controller
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Bind password fields for synchronization
        visiblePasswordField.textProperty().bindBidirectional(hiddenPasswordField.textProperty());

        // Initially hide visible password field
        visiblePasswordField.setVisible(false);
        hiddenPasswordField.setVisible(true);
    }

    // Private helper methods

    /**
     * Get password from the appropriate field
     */
    private String getPasswordFromFields() {
        return showPasswordCheckBox.isSelected()
            ? visiblePasswordField.getText()
            : hiddenPasswordField.getText();
    }

    /**
     * Validate user input
     */
    private boolean validateInput(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập email!");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập mật khẩu!");
            return false;
        }

        return true;
    }

    /**
     * Navigate to home screen after successful login
     */
    private void navigateToHome(ActionEvent event, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
        root = loader.load();

        // Pass user information to home controller
        HomeController homeController = loader.getController();
        homeController.displayName(user.getEmail());

        // Switch scene
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Show error alert to user
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
