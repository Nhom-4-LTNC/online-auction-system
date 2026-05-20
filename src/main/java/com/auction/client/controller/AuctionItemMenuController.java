package com.auction.client.controller;

import com.auction.client.network.Client;
import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.VehicleDTO;
import com.auction.server.model.auction.Auction;
import com.auction.shared.enums.ItemType;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.util.SceneUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class AuctionItemMenuController implements Initializable {

    private final Client client = Client.getInstance();

    @FXML private TextField startingPriceTF;
    @FXML private TextField descriptionTF;
    // Nếu có thể, nên thêm nameTF trong FXML.
    // @FXML private TextField nameTF;

    @FXML private Label itemTypeLabel;
    @FXML private Button backButton;
    @FXML private Button auctionButton;

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;
    @FXML private RadioButton otherButton;

    @FXML private AnchorPane electronicsPane;
    @FXML private TextField electronicsBrandTF;
    @FXML private TextField warrantyTF;

    @FXML private AnchorPane artPane;
    @FXML private TextField authorTF;
    @FXML private TextField genreTF;

    @FXML private AnchorPane vehiclePane;
    @FXML private TextField vehicleBrandTF;
    @FXML private TextField vinTF;
    @FXML private TextField mileageTF;

    private ItemType currentType;
    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client.connect();
        registerServerMessageHandler();

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
    public void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(auctionButton.getScene().getWindow());

        if (file != null) {
            selectedImageFile = file;
        }
    }

    @FXML
    public void createAuction(ActionEvent event) {
        if (!client.isConnected()) {
            showAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        try {
            ItemDTO itemDto = buildItemDTO();

            long now = System.currentTimeMillis();
            long endTime = now + 60L * 60 * 1000;

            CreateAuctionRequest payload = new CreateAuctionRequest(
                    itemDto,
                    itemDto.getStartingPrice(),
                    Auction.DEFAULT_BID_STEP,
                    now,
                    endTime
            );

            Request<CreateAuctionRequest> request = new Request<>(
                    ActionType.CREATE_AUCTION,
                    payload
            );

            client.sendMessage(request);

        } catch (IllegalArgumentException e) {
            showAlert("Lỗi nhập liệu", e.getMessage());

        } catch (IOException e) {
            showAlert("Lỗi ảnh", "Không thể đọc ảnh sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void registerServerMessageHandler() {
        client.setOnMessageReceived(message -> {
            if (!(message instanceof Response<?> response)) {
                return;
            }

            if (response.getAction() != ActionType.CREATE_AUCTION) {
                return;
            }

            Platform.runLater(() -> handleCreateAuctionResponse(response));
        });
    }

    private void handleCreateAuctionResponse(Response<?> response) {
        if (!response.isSuccess()) {
            showAlert("Tạo phiên thất bại", response.getErrorMessage());
            return;
        }

        Object payload = response.getPayload();

        if (!(payload instanceof CreateAuctionResponse createAuctionResponse)) {
            showAlert("Lỗi", "Phản hồi tạo phiên không đúng định dạng.");
            return;
        }

        showInfo("Tạo phiên thành công", createAuctionResponse.getMessage());
    }

    private ItemDTO buildItemDTO() throws IOException {
        if (currentType == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại sản phẩm!");
        }

        double startingPrice = parsePositiveDouble(
                startingPriceTF.getText(),
                "Giá khởi đầu"
        );

        String description = descriptionTF.getText().trim();

        /*
         * Tốt nhất nên có nameTF riêng.
         * Nếu chưa có, tạm đặt name theo loại sản phẩm.
         */
        String name = currentType.name();

        ImagePayload imagePayload = readSelectedImage();

        return switch (currentType) {
            case ELECTRONICS -> new ElectronicsDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(electronicsBrandTF, "Thương hiệu"),
                    parsePositiveInt(warrantyTF.getText(), "Thời gian bảo hành")
            );

            case ART -> new ArtDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(authorTF, "Tác giả"),
                    parsePositiveInt(genreTF.getText(), "Năm sáng tác")
            );

            case VEHICLE -> new VehicleDTO(
                    name,
                    description,
                    startingPrice,
                    imagePayload.imageData(),
                    imagePayload.imageFileName(),
                    requireText(vehicleBrandTF, "Thương hiệu xe"),
                    requireText(vinTF, "VIN"),
                    parsePositiveInt(mileageTF.getText(), "Số km đã đi")
            );
        };
    }

    private ImagePayload readSelectedImage() throws IOException {
        if (selectedImageFile == null) {
            return new ImagePayload(null, null);
        }

        byte[] imageData = Files.readAllBytes(selectedImageFile.toPath());
        String imageFileName = selectedImageFile.getName();

        return new ImagePayload(imageData, imageFileName);
    }

    private void updateItemTypePanels() {
        boolean isElectronics = currentType == ItemType.ELECTRONICS;
        boolean isArt = currentType == ItemType.ART;
        boolean isVehicle = currentType == ItemType.VEHICLE;

        electronicsPane.setVisible(isElectronics);
        electronicsPane.setManaged(isElectronics);

        artPane.setVisible(isArt);
        artPane.setManaged(isArt);

        vehiclePane.setVisible(isVehicle);
        vehiclePane.setManaged(isVehicle);

        itemTypeLabel.setText(currentType == null ? "CHƯA CHỌN" : currentType.name());
    }

    private double parsePositiveDouble(String rawValue, String fieldName) {
        try {
            double value = Double.parseDouble(rawValue.trim());

            if (value <= 0) {
                throw new NumberFormatException();
            }

            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là số dương!");
        }
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        try {
            int value = Integer.parseInt(rawValue.trim());

            if (value <= 0) {
                throw new NumberFormatException();
            }

            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên dương!");
        }
    }

    private String requireText(TextField textField, String fieldName) {
        String value = textField.getText().trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống!");
        }

        return value;
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

    private record ImagePayload(byte[] imageData, String imageFileName) {
    }
}