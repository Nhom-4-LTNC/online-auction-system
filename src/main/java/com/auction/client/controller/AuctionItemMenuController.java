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
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.*;
import java.util.ResourceBundle;

public class AuctionItemMenuController implements Initializable {

    private static final double DEFAULT_BID_STEP = 10.0;

    private final AuctionClientService auctionClientService = new AuctionClientService();

    @FXML private TextField itemNameTF;
    @FXML private TextField startingPriceTF;
    @FXML private TextField bidStepTF;
    @FXML private TextField descriptionTF;
    @FXML private Label itemTypeLabel;
    @FXML private Label selectedImageFileLabel;
    @FXML private Button auctionButton;

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

    @FXML private DatePicker startTime;
    @FXML private ComboBox<String> startHourComboBox;
    @FXML private ComboBox<String> startMinuteComboBox;
    @FXML private DatePicker endTime;
    @FXML private ComboBox<String> endHourComboBox;
    @FXML private ComboBox<String> endMinuteComboBox;

    private ItemType currentType;
    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentType = null;
        setupTimeComboBoxes();
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
            selectedImageFile = file;
            selectedImageFileLabel.setText(file.getName());
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
        double bidStep = parseOptionalPositiveDouble(bidStepTF.getText(), DEFAULT_BID_STEP, "Bid step");

        long startLong = resolveStartTimeMillis();
        long endLong = resolveEndTimeMillis();

        if (endLong <= startLong) {
            throw new InvalidAuctionDate("End time must be after the start time.");
        }

        return new CreateAuctionRequest(
                itemDto,
                itemDto.getStartingPrice(),
                bidStep,
                startLong,
                endLong
        );
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

        itemTypeLabel.setText(currentType == null ? "Choose a Type!" : currentType.name());
    }

    private void setupTimeComboBoxes() {
        startHourComboBox.setItems(FXCollections.observableArrayList(formatRange(0, 23)));
        endHourComboBox.setItems(FXCollections.observableArrayList(formatRange(0, 23)));
        startMinuteComboBox.setItems(FXCollections.observableArrayList(formatRange(0, 59)));
        endMinuteComboBox.setItems(FXCollections.observableArrayList(formatRange(0, 59)));
    }

    private String[] formatRange(int start, int end) {
        String[] values = new String[end - start + 1];
        for (int value = start; value <= end; value++) {
            values[value - start] = String.format("%02d", value);
        }
        return values;
    }

    private long resolveStartTimeMillis() {
        if (startTime.getValue() == null
                || startHourComboBox.getValue() == null
                || startMinuteComboBox.getValue() == null) {
            return Instant.now().toEpochMilli();
        }

        return toEpochMillis(
                startTime.getValue(),
                startHourComboBox.getValue(),
                startMinuteComboBox.getValue()
        );
    }

    private long resolveEndTimeMillis() {
        if (endTime.getValue() == null
                || endHourComboBox.getValue() == null
                || endMinuteComboBox.getValue() == null) {
            throw new InvalidAuctionDate("Please select an end date, hour, and minute.");
        }

        return toEpochMillis(
                endTime.getValue(),
                endHourComboBox.getValue(),
                endMinuteComboBox.getValue()
        );
    }

    private long toEpochMillis(LocalDate date, String hour, String minute) {
        return LocalDateTime.of(date, LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute)))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
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

    private double parseOptionalPositiveDouble(String rawValue, double defaultValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        return parsePositiveDouble(rawValue, fieldName);
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
