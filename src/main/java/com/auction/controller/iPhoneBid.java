package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class iPhoneBid implements Initializable {
    private double startingBid = 2530;

    @FXML
    Button button;
    @FXML
    TextField valueTextField;
    @FXML
    Label priceLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceLabel.setText("Price: $"+startingBid );
    }

    public void Bid(ActionEvent event) throws IOException {
        String price = valueTextField.getText();

        try{
            int value = Integer.parseInt(price);
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
}

