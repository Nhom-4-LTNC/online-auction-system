package com.auction.client.controller;

import java.io.IOException;

import com.auction.client.Client;
import com.auction.dto.ArtDTO;
import com.auction.dto.ElectronicsDTO;
import com.auction.dto.ItemDTO;
import com.auction.dto.VehicleDTO;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.protocol.ActionType;
import com.auction.protocol.auction.AuctionResponse;
import com.auction.protocol.auction.CreateAuctionRequest;
import com.auction.util.SceneUtils;
import com.auction.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class AuctionItemMenuController {

    private final Client client = Client.getInstance();

    // --- Trường chung ---
    @FXML TextField startingPriceTF;
    @FXML TextField descriptionTF;
    @FXML Label itemTypeLabel;
    @FXML Button backButton;
    @FXML Button auctionButton;

    String currentType = "Other";

    // --- Radio buttons chọn loại sản phẩm ---
    @FXML RadioButton electronicsButton, artButton, vehicleButton, otherButton;

    // --- Electronics ---
    @FXML AnchorPane electronicsPane;
    @FXML TextField electronicsBrandTF;
    @FXML TextField warrantyTF;

    // --- Art ---
    @FXML AnchorPane artPane;
    @FXML TextField authorTF;
    @FXML TextField genreTF;   // nhập năm sáng tác (yearCreated)

    // --- Vehicle ---
    @FXML AnchorPane vehiclePane;
    @FXML TextField vehicleBrandTF;
    @FXML TextField vinTF;
    @FXML TextField mileageTF;

    // ----------------------------------------------------------------

    @FXML
    public void onItemSelected(ActionEvent event) {
        electronicsPane.setVisible(electronicsButton.isSelected());
        artPane.setVisible(artButton.isSelected());
        vehiclePane.setVisible(vehicleButton.isSelected());

        if (electronicsButton.isSelected()) currentType = "Electronics";
        else if (artButton.isSelected()) currentType = "Art";
        else if (vehicleButton.isSelected()) currentType = "Vehicle";
        else currentType = "Other";

        itemTypeLabel.setText(currentType.toUpperCase());
    }

    @FXML
    public void createAuction(ActionEvent event) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Vui lòng đăng nhập trước!");
            return;
        }

        // 1. Đọc và kiểm tra giá khởi đầu
        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startingPriceTF.getText().trim());
            if (startingPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Giá khởi đầu phải là số dương!");
            return;
        }

        String description = descriptionTF.getText().trim();
        String name = description.isEmpty() ? currentType : description;

        // 2. Xây dựng DTO cụ thể theo loại sản phẩm được chọn
        ItemDTO itemDto;
        try {
            switch (currentType) {
                case "Electronics" -> {
                    int warranty = Integer.parseInt(warrantyTF.getText().trim());
                    itemDto = new ElectronicsDTO(name, description, startingPrice,
                            electronicsBrandTF.getText().trim(), warranty);
                }
                case "Art" -> {
                    int year = Integer.parseInt(genreTF.getText().trim());
                    itemDto = new ArtDTO(name, description, startingPrice,
                            authorTF.getText().trim(), year);
                }
                case "Vehicle" -> {
                    int mileage = Integer.parseInt(mileageTF.getText().trim());
                    itemDto = new VehicleDTO(name, description, startingPrice,
                            vehicleBrandTF.getText().trim(), vinTF.getText().trim(), mileage);
                }
                default -> {
                    showAlert("Lỗi", "Vui lòng chọn loại sản phẩm!");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng nhập đúng định dạng số cho các trường số!");
            return;
        }

        // 3. Kiểm tra kết nối trước khi gửi
        if (!client.isConnected()) {
            showAlert("Lỗi kết nối", "Không thể kết nối tới server. Vui lòng thử lại!");
            return;
        }

        long now = System.currentTimeMillis();
        long endTime = now + 60L * 60 * 1000; // mặc định: phiên kéo dài 1 tiếng

        // 4. Đăng ký xử lý phản hồi từ server
        // Lưu ý: setOnMessageReceived bị ghi đè mỗi lần mở màn hình/gọi action.
        // Ít nhất: bỏ qua các response không liên quan tới “create auction”.
        client.setOnMessageReceived(response -> {
            if (!(response instanceof AuctionResponse auctionResponse)) return;

            if (auctionResponse.getResponseType() == ActionType.CREATE_AUCTION_SUCCESS) {
                Auction created = auctionResponse.getAuction();
                showInfo("Tạo thành công",
                        "Phiên đấu giá #" + created.getId() + " đã được tạo!\n"
                                + "Giá khởi đầu: " + created.getStartPrice());
            } else if (auctionResponse.getResponseType() == ActionType.CREATE_AUCTION_FAILURE) {
                showAlert("Tạo phiên thất bại", auctionResponse.getMessage());
            }
        });

        // 5. Gửi yêu cầu tạo phiên đấu giá lên server
        client.sendMessage(new CreateAuctionRequest(
                currentUser.getId(), itemDto, Auction.DEFAULT_BID_STEP, now, endTime));
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    // ----------------------------------------------------------------
    // HELPER
    // ----------------------------------------------------------------

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
}

