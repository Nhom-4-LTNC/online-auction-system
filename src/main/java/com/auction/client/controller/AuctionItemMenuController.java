package com.auction.client.controller;

import com.auction.model.auction.Auction;
import com.auction.model.item.ItemType;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuctionItemMenuController {

    // Dùng singleton để cả ứng dụng dùng chung một AuctionService
    private final AuctionService auctionService = AuctionService.getInstance();

    // --- Trường chung ---
    @FXML TextField startingPriceTF;
    @FXML TextField descriptionTF;
    @FXML Label    itemTypeLabel;
    @FXML Button   backButton;
    @FXML Button   auctionButton;

    String currentType = "Other";

    // --- Radio buttons chọn loại sản phẩm ---
    @FXML RadioButton electronicsButton, artButton, vehicleButton, otherButton;

    public void onItemSelected(ActionEvent event) {
        electronicsPane.setVisible(electronicsButton.isSelected());
        artPane.setVisible(artButton.isSelected());
        vehiclePane.setVisible(vehicleButton.isSelected());

        if      (electronicsButton.isSelected()) currentType = "Electronics";
        else if (artButton.isSelected())         currentType = "Art";
        else if (vehicleButton.isSelected())     currentType = "Vehicle";
        else                                     currentType = "Other";

        itemTypeLabel.setText(currentType.toUpperCase());
    }

    // TẠO PHIÊN ĐẤU GIÁ — kết nối form với AuctionService
    public void createAuction(ActionEvent event) {
        // 1. Lấy user đang đăng nhập
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Vui lòng đăng nhập trước!");
            return;
        }

        // 2. Đọc và kiểm tra giá khởi đầu
        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startingPriceTF.getText().trim());
            if (startingPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Giá khởi đầu phải là số dương!");
            return;
        }

        String description = descriptionTF.getText().trim();

        // 3. Xây dựng Map thông tin sản phẩm theo từng loại
        Map<String, Object> itemData = new HashMap<>();
        // Dùng description làm tên sản phẩm (nếu muốn có tên riêng, thêm TextField vào FXML)
        itemData.put("name",  description.isEmpty() ? currentType : description);
        itemData.put("desc",  description);
        itemData.put("price", startingPrice);

        ItemType itemType;

        try {
            switch (currentType) {
                case "Electronics" -> {
                    itemType = ItemType.ELECTRONICS;
                    itemData.put("brand",    electronicsBrandTF.getText().trim());
                    itemData.put("warranty", Integer.parseInt(warrantyTF.getText().trim()));
                }
                case "Art" -> {
                    itemType = ItemType.ART;
                    itemData.put("artist", authorTF.getText().trim());
                    itemData.put("year", Integer.parseInt(genreTF.getText().trim()));
                }
                case "Vehicle" -> {
                    itemType = ItemType.VEHICLE;
                    itemData.put("brand",   vehicleBrandTF.getText().trim());
                    itemData.put("vin",     vinTF.getText().trim());
                    itemData.put("mileage", Integer.parseInt(mileageTF.getText().trim()));
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

        // 4. Gọi AuctionService để tạo phiên đấu giá
        try {
            long now     = System.currentTimeMillis();
            long endTime = now + 60L * 60 * 1000; // mặc định: phiên kéo dài 1 tiếng

            Auction newAuction = auctionService.createAuction(
                    currentUser, itemType, itemData,
                    Auction.DEFAULT_BID_STEP, now, endTime
            );

            showInfo("Tạo thành công",
                    "Phiên đấu giá #" + newAuction.getId() + " đã được tạo!\n"
                    + "Giá khởi đầu: " + startingPrice);

        } catch (Exception e) {
            showAlert("Lỗi tạo phiên đấu giá", e.getMessage());
        }
    }

    public void back(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/HomeScreen.fxml")));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // HELPER — hiển thị thông báo

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

    // --- Electronics ---
    @FXML AnchorPane electronicsPane;
    @FXML TextField  electronicsBrandTF;
    @FXML TextField  warrantyTF;

    // --- Art ---
    @FXML AnchorPane artPane;
    @FXML TextField  authorTF;
    @FXML TextField  genreTF; // nhập năm sáng tác (yearCreated)

    // --- Vehicle ---
    @FXML AnchorPane vehiclePane;
    @FXML TextField  vehicleBrandTF;
    @FXML TextField  vinTF;
    @FXML TextField  mileageTF;
}
