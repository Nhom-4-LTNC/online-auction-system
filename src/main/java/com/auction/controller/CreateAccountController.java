package com.auction.controller;

import com.auction.model.Bidder;
import com.auction.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class CreateAccountController {
    @FXML
    Button Create;
    @FXML
    TextField passTextField;
    @FXML
    TextField emailTextField;
    @FXML
    TextField nameTextField;

    Stage stage;
    Scene scene;
    Parent root;

    public void Create(ActionEvent event) throws IOException {
        String email = emailTextField.getText();
        String pass = passTextField.getText();
        String username = nameTextField.getText();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/controller/example/demo10/fxml/HomeScreen.fxml"));
        root = loader.load();
        HomeController homeController = loader.getController();
        homeController.displayName(email);
        homeController.displayPass(pass);

        System.out.println("Tạo tài khoản thành công");

        User bidder = new Bidder(username, pass, email);
        UserData.users.put(bidder.getEmail(), bidder.getPwd());

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
}
