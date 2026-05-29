package com.auction.client.controller;

import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ClientServiceException;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.VehicleDTO;
import com.auction.shared.enums.ItemType;
import com.auction.shared.exception.InvalidAuctionDate;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class AuctionItemMenuController implements Initializable {

    private static final long DEFAULT_AUCTION_DURATION_MILLIS = 60L * 60 * 1000;
    private static final int DEFAULT_AUCTION_EXTENSION_SECONDS = 10;

    private final AuctionClientService auctionClientService = new AuctionClientService();

    @FXML private TextField itemNameTF;
    @FXML private TextField startingPriceTF;
    @FXML private TextField descriptionTF;
    @FXML private Label itemTypeLabel;
    @FXML private Button auctionButton;
    @FXML private Button importImageButton;

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;

    @FXML private VBox electronicsPane;
    @FXML private TextField electronicsBrandTF;
    @FXML private TextField warrantyTF;

    @FXML private VBox artPane;
    @FXML private TextField authorTF;
    @FXML private TextField yearTF;

    @FXML private VBox vehiclePane;
    @FXML private TextField vehicleBrandTF;
    @FXML private TextField vinTF;
    @FXML private TextField mileageTF;

    @FXML private ImageView previewImage;

    @FXML private DatePicker startTime;
    @FXML private TextField HHMMSSstartTime;
    @FXML private DatePicker endTime;
    @FXML private TextField HHMMSSendTime;

    private ItemType currentType;
    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentType = null;
        updateItemTypePanels();
    }

    @FXML
    public void onItemSelected(ActionEvent event) {
        if (electronicsButton.isSelected()) {
            currentType = ItemType.ELECTRONICS;
        } else if (artButton.isSelected()) {
            currentType = ItemType.ART;
        } else if (vehicleButton.isSelected()) {
            currentType = ItemType.VEHICLE;
        } else {
            currentType = null;
        }

        updateItemTypePanels();
    }

    @FXML
    public void importImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose item image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(auctionButton.getScene().getWindow());
        if (file != null) {
            // file exists!
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            previewImage.setImage(image);
        }
    }

    @FXML
    public void createAuction(ActionEvent event) {
        CreateAuctionRequest request;
        try {
            request = buildCreateAuctionRequest();
        } catch (IllegalArgumentException e) {
            AlertUtils.showError("Input error", e.getMessage());
            return;
        } catch (IOException e) {
            AlertUtils.showError("Image error", "Cannot read item image: " + e.getMessage());
            return;
        }

        submitCreateAuction(event, request);
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void submitCreateAuction(ActionEvent event, CreateAuctionRequest request) {
        setSubmitting(true);

        Task<CreateAuctionResponse> task = new Task<>() {
            @Override
            protected CreateAuctionResponse call() {
                return auctionClientService.createAuction(request);
            }
        };

        task.setOnSucceeded(workerEvent -> {
            setSubmitting(false);
            CreateAuctionResponse response = task.getValue();
            AlertUtils.showInfo("Create auction success", response == null ? "Auction created." : response.getMessage());
            try {
                SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
            } catch (IOException e) {
                AlertUtils.showError("Navigation error", "Cannot open auction list: " + e.getMessage());
            }
        });

        task.setOnFailed(workerEvent -> {
            setSubmitting(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Cannot create auction.";
            AlertUtils.showError("Create auction failed", message);
        });

        Thread thread = new Thread(task, "create-auction-submit");
        thread.setDaemon(true);
        thread.start();
    }

    private CreateAuctionRequest buildCreateAuctionRequest() throws IOException {
        ItemDTO itemDto = buildItemDTO();

        //long now = System.currentTimeMillis();
        //long endTime = now + DEFAULT_AUCTION_DURATION_MILLIS;

        // 1. Explicitly check for null dates to prevent NullPointerException
        if (startTime.getValue() == null || endTime.getValue() == null) {
            // Stop execution and tell the user they missed a field
            throw new InvalidAuctionDate("Please select both a start and end date.");
        }

        try {
            // 2. Attempt to parse the text fields.
            // This is where DateTimeParseException can happen.
            LocalTime exactStartLocalTime = LocalTime.parse(HHMMSSstartTime.getText().trim());
            LocalTime exactEndLocalTime = LocalTime.parse(HHMMSSendTime.getText().trim());

            // 3. Combine the Date (from DatePicker) and Time (from parsing) cleanly
            long startLong = LocalDateTime.of(startTime.getValue(), exactStartLocalTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            long endLong = LocalDateTime.of(endTime.getValue(), exactEndLocalTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            long currentMillis = Instant.now().toEpochMilli();

            if  (!( // NOT
                    (currentMillis < startLong)
                            && (startLong < endLong)
            )) {
                throw new InvalidAuctionDate("Invalid auction date.");
            }


            return new CreateAuctionRequest(
                    itemDto,
                    itemDto.getStartingPrice(),
                    DEFAULT_AUCTION_EXTENSION_SECONDS,
                    startLong,
                    endLong
            );

        } catch (DateTimeParseException e) {
            // 4. Catch the error if the user typed something like "hello" or "12:0"
            throw new InvalidAuctionDate("Invalid time format. Please use HH:mm:ss");
            // e.g., AlertUtils.showError("Input error", "Please use valid 24-hour time formats.");
        }
    }

    private ItemDTO buildItemDTO() throws IOException {
        if (currentType == null) {
            throw new IllegalArgumentException("Please select item type.");
        }

        String name = itemNameTF.getText();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Please enter an item name.");
        }
        double startingPrice = parsePositiveDouble(startingPriceTF.getText(), "Starting price");
        String description = descriptionTF.getText().trim();
        ImagePayload imagePayload = readSelectedImage();

        return switch (currentType) {
            case ELECTRONICS -> new ElectronicsDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(electronicsBrandTF, "Brand"),
                    parsePositiveInt(warrantyTF.getText(), "Warranty")
            );

            case ART -> new ArtDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(authorTF, "Author"),
                    parsePositiveInt(yearTF.getText(), "Year")
            );

            case VEHICLE -> new VehicleDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(vehicleBrandTF, "Vehicle brand"),
                    requireText(vinTF, "VIN"),
                    parsePositiveInt(mileageTF.getText(), "Mileage")
            );
        };
    }

    private ImagePayload readSelectedImage() throws IOException {
        if (selectedImageFile == null) {
            return new ImagePayload(null, null);
        }

        byte[] imageData = Files.readAllBytes(selectedImageFile.toPath());
        return new ImagePayload(imageData, selectedImageFile.getName());
    }

    private void updateItemTypePanels() {
        boolean isElectronics = currentType == ItemType.ELECTRONICS;
        boolean isArt = currentType == ItemType.ART;
        boolean isVehicle = currentType == ItemType.VEHICLE;

        setPanelVisible(electronicsPane, isElectronics);
        setPanelVisible(artPane, isArt);
        setPanelVisible(vehiclePane, isVehicle);

        previewImage.setImage(null);

        itemTypeLabel.setText(currentType == null ? "Choose a Type!" : currentType.name());
    }

    private void setPanelVisible(VBox panel, boolean visible) {
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    private double parsePositiveDouble(String rawValue, String fieldName) {
        try {
            double value = Double.parseDouble(rawValue.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive number.");
        }
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer.");
        }
    }

    private String requireText(TextField textField, String fieldName) {
        String value = textField.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private void setSubmitting(boolean submitting) {
        auctionButton.setDisable(submitting);
    }

    private record ImagePayload(byte[] imageData, String imageFileName) {
    }
}
