package com.auction.controller;

import com.auction.dao.UserDAO;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Objects;

public class AuctionItemMenuController {

    // generals
    @FXML
    TextField startingPriceTF;
    @FXML
    TextField descriptionTF;
    @FXML
    Label itemTypeLabel;
    @FXML
    Button backButton;
    @FXML
    Button auctionButton;

    String currentType = "Other"; // default

    // radio buttons
    @FXML
    RadioButton electronicsButton,artButton,vehicleButton,otherButton;

    public void onItemSelected(ActionEvent event) {
        electronicsPane.setVisible(electronicsButton.isSelected());
        artPane.setVisible(artButton.isSelected());
        vehiclePane.setVisible(vehicleButton.isSelected());

        if (electronicsButton.isSelected()) {currentType = "Electronics";}
        else if (artButton.isSelected()) {currentType = "Art";}
        else if (vehicleButton.isSelected()) {currentType = "Vehicle";}
        else {currentType = "Other";}

        itemTypeLabel.setText(currentType.toUpperCase());
    }

    public void createAuction(ActionEvent event) {
        double startingPrice = Double.parseDouble(startingPriceTF.getText());
        String description = descriptionTF.getText();


        if (currentType.equals("Electronics")) {
            String electronicBrand = electronicsBrandTF.getText();
            String warranty = warrantyTF.getText();
        } else if (currentType.equals("Art")) {
            String author = authorTF.getText();
            String genre = genreTF.getText();
        } else if (currentType.equals("Vehicle")) {
            String brand = vehicleBrandTF.getText();
            String vin = vinTF.getText();
            String mileage = mileageTF.getText();
        } else {

        }

        // IMPORTANT: CREATE AUCTION HERE
        // SOMEONE PLS HELP

    }

    public void back(ActionEvent event) throws IOException {
        new FXMLLoader();
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/HomeScreen.fxml")));
        Parent root = loader.load();
        Scene scene = new Scene((root));
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    //electronics
    @FXML
    AnchorPane electronicsPane;
    @FXML
    TextField electronicsBrandTF;
    @FXML
    TextField warrantyTF;

    //art
    @FXML
    AnchorPane artPane;
    @FXML
    TextField authorTF;
    @FXML
    TextField genreTF;

    //vehicle
    @FXML
    AnchorPane vehiclePane;
    @FXML
    TextField vehicleBrandTF;
    @FXML
    TextField vinTF;
    @FXML
    TextField mileageTF;

}
