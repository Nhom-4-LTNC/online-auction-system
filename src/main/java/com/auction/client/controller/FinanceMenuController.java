package com.auction.client.controller;

import java.io.IOException;

import com.auction.shared.util.SceneUtils;
import com.auction.shared.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class FinanceMenuController {

    @FXML private TextField incrementBalanceTextField;
    @FXML private TextField setBalanceTextField;
    @FXML private Button incrementBalanceButton;
    @FXML private Button setBalanceButton;
    @FXML private Button backButton;

    /*
     * REVIEW - VẤN ĐỀ KIẾN TRÚC NGHIÊM TRỌNG:
     *
     * Class này thuộc package client.controller, nên không được gọi trực tiếp UserService phía server.
     *
     * Nếu UserService ở đây là:
     *
     * com.auction.server.service.UserService
     *
     * thì đây là vi phạm kiến trúc Client-Server.
     *
     * Flow sai hiện tại:
     *
     * JavaFX Controller
     *   -> server.service.UserService
     *   -> Repository
     *   -> Database
     *
     * Flow đúng phải là:
     *
     * JavaFX Controller
     *   -> UserClientService hoặc FinanceClientService phía client
     *   -> gửi Request qua socket
     *   -> Server/ClientHandler
     *   -> UserController hoặc FinanceController phía server
     *   -> UserService/PaymentService phía server
     *   -> Repository
     *   -> Database
     *
     * Lý do cần sửa:
     * - Client không được truy cập server service/repository/database trực tiếp.
     * - Nếu gọi trực tiếp service, sẽ bỏ qua socket, Request/Response protocol và session currentUser trên server.
     * - Khi client và server chạy ở hai máy khác nhau, code này sẽ không hoạt động đúng.
     * - Việc cập nhật balance là thao tác nhạy cảm, phải được server kiểm tra quyền và session.
     *
     * Hướng sửa:
     * - Tạo com.auction.client.service.UserClientService hoặc FinanceClientService.
     * - Controller gọi client-side service.
     * - Client-side service gửi Request qua socket, ví dụ ActionType.ADD_BALANCE hoặc UPDATE_BALANCE.
     * - Server kiểm tra currentUser từ ClientHandler/session, không tin user object từ client gửi lên.
     */

    @FXML
    public void incrementBalance(ActionEvent event) {
        try {
            String input = incrementBalanceTextField.getText().trim();
            if (input.isEmpty()) {
                showAlert("Error", "Please enter an amount!");
                return;
            }

            double amount = Double.parseDouble(input);

            /*
             * REVIEW:
             * SessionManager phía client chỉ nên dùng để lưu thông tin hiển thị hiện tại.
             * Không nên dùng currentUser object phía client làm căn cứ bảo mật để cập nhật DB.
             *
             * Server phải xác định user hiện tại thông qua session trong ClientHandler.
             * Client chỉ nên gửi amount cần nạp/cập nhật, không gửi user object để server tin tuyệt đối.
             */
            var currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                showAlert("Error", "Please login first!");
                return;
            }

            /*
             * REVIEW - LỖI DO MODEL ĐÃ THAY ĐỔI:
             *
             * Thiết kế User hiện tại đã bỏ BidderProfile và SellerProfile.
             * Balance đã được đưa trực tiếp lên User.
             *
             * Vì vậy code này đã lỗi thời:
             *
             * currentUser.getBidderProfile()
             *
             * Cách đúng theo model mới:
             *
             * double newBalance = currentUser.getBalance() + amount;
             *
             * Tuy nhiên cần lưu ý:
             * - Client chỉ tính toán để hiển thị tạm được.
             * - Giá trị chính thức phải do server cập nhật và trả về.
             */

            /*
             * REVIEW:
             * Với chức năng "increment balance", amount nên bắt buộc > 0.
             * Hiện tại nếu nhập amount âm, code sẽ biến increment thành trừ tiền.
             *
             * Nếu muốn có chức năng rút/trừ tiền, nên làm action riêng và server phải kiểm tra quyền.
             */

            /*
             * REVIEW - VẤN ĐỀ BẢO MẬT/NGHIỆP VỤ:
             *
             * Không nên để client set trực tiếp balance tuyệt đối.
             * Nếu người dùng tự nhập số, họ có thể set balance thành bất kỳ giá trị nào.
             *
             * Nếu đây là chức năng "nạp tiền giả lập" cho demo, nên đặt tên rõ là ADD_BALANCE
             * và server chỉ cộng amount vào user hiện tại.
             *
             * Không nên truyền currentUser object từ client vào service để update DB.
             * Server nên lấy user hiện tại từ session server-side.
             */

            /*
             * REVIEW:
             * Sau khi server cập nhật thành công, nên cập nhật lại SessionManager.currentUser.balance
             * bằng giá trị server trả về, tránh client hiển thị balance cũ.
             */
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number!");
        } catch (Exception e) {
            /*
             * REVIEW:
             * Không nên hiển thị trực tiếp e.getMessage() nếu message có thể đến từ SQLException hoặc lỗi nội bộ.
             * Client nên hiển thị message thân thiện từ Response.error.
             */
            showAlert("Error", "Failed to update balance: " + e.getMessage());
        }
    }

    @FXML
    public void setBalance(ActionEvent event) {
        try {
            String input = setBalanceTextField.getText().trim();
            if (input.isEmpty()) {
                showAlert("Error", "Please enter an amount!");
                return;
            }

            double amount = Double.parseDouble(input);
            var currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                showAlert("Error", "Please login first!");
                return;
            }

            if (amount < 0) {
                showAlert("Error", "Balance cannot be negative!");
                return;
            }

            /*
             * REVIEW - VẤN ĐỀ BẢO MẬT NGHIÊM TRỌNG:
             *
             * Không nên cho user thường tự set balance tuyệt đối.
             * Đây là thao tác admin hoặc thao tác test/demo.
             *
             * Nếu chức năng này để người dùng tự nạp tiền, nên đổi thành:
             *
             * addBalance(amount)
             *
             * Không phải:
             *
             * setBalance(amount)
             *
             * Vì setBalance cho phép client tự đặt balance thành 1 tỷ chỉ bằng nhập input.
             *
             * Nếu vẫn cần setBalance:
             * - Chỉ ADMIN được gọi.
             * - Server phải kiểm tra role ADMIN từ session server-side.
             */

            showInfo("Success", String.format("Balance set to: %.2f", amount));
            setBalanceTextField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number!");
        } catch (Exception e) {
            showAlert("Error", "Failed to set balance: " + e.getMessage());
        }
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        /*
         * REVIEW:
         * SceneUtils đang nằm trong shared.util thì cần xem lại boundary.
         * Nếu SceneUtils dùng JavaFX thì nó nên nằm trong client.util, không nên nằm trong shared.
         * shared nên chỉ chứa DTO/protocol/enum/exception dùng chung client-server.
         */
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void showAlert(String title, String message) {
        /*
         * REVIEW:
         * Có thể tách Alert helper sang client.util.AlertUtils để tránh lặp code ở nhiều controller.
         * Nhưng đây là vấn đề nhỏ, chưa cần ưu tiên nếu đang gấp demo.
         */
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