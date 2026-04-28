package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class iPhoneBid implements Initializable {
    private double startingBid = 2530.0;

    @FXML
    Button button;
    @FXML
    TextField valueTextField;
    @FXML
    Label priceLabel;

    Stage stage;
    Scene scene;
    Parent root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceLabel.setText("Price: $"+startingBid );
    }

    public void Bid(ActionEvent event) throws IOException {
        String price = valueTextField.getText();

        try{
            double value = Double.parseDouble(price);
            if (value > startingBid){
                priceLabel.setText("Price: $"+price);
                valueTextField.clear();

                startingBid = value;
            }else{
                System.out.println("the bid price is lower than the product price");
            }
        } catch (NumberFormatException e) {
            e.getStackTrace();
        }
    }

    @FXML
    Button out;
    public void OutBid(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/controller/example/demo10/fxml/AuctionMenu.fxml"));
        root = loader.load();
        scene = new Scene(root);
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }
}

