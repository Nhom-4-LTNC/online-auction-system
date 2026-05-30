package com.auction.client.controller;

import com.auction.client.service.ClientServiceException;
import com.auction.client.service.WalletClientService;
import com.auction.client.session.ClientSession;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.FormatUtils;
import com.auction.client.util.SceneUtils;
import com.auction.shared.dto.BalanceResponse;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FinanceMenuController implements Initializable {

    private final WalletClientService walletClientService = new WalletClientService();

    @FXML private TextField incrementBalanceTextField;
    @FXML private TextField setBalanceTextField;
    @FXML private Button incrementBalanceButton;
    @FXML private Button setBalanceButton;
    @FXML private Button backButton;
    @FXML private Label currentBalanceLabel;
    @FXML private Label availableBalanceLabel;
    @FXML private Label unpaidWinningAmountLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        renderWalletSummary();
        setBalanceButton.setDisable(true);
        setBalanceTextField.setDisable(true);
    }

    @FXML
    public void incrementBalance(ActionEvent event) {
        if (!ClientSession.isLoggedIn()) {
            AlertUtils.showError("Ví tiền", "Vui lòng đăng nhập trước.");
            return;
        }

        double amount;
        try {
            amount = parsePositiveAmount(incrementBalanceTextField.getText());
        } catch (IllegalArgumentException e) {
            AlertUtils.showError("Dữ liệu không hợp lệ", e.getMessage());
            return;
        }

        submitAddBalance(amount);
    }

    @FXML
    public void setBalance(ActionEvent event) {
        AlertUtils.showWarning("Ví tiền", "Không hỗ trợ đặt số dư trực tiếp. Vui lòng dùng nạp tiền.");
    }

    @FXML
    public void back(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");
    }

    private void submitAddBalance(double amount) {
        setSubmitting(true);

        Task<BalanceResponse> task = new Task<>() {
            @Override
            protected BalanceResponse call() {
                return walletClientService.addBalance(amount);
            }
        };

        task.setOnSucceeded(event -> {
            setSubmitting(false);
            BalanceResponse response = task.getValue();
            updateSessionWallet(response);
            renderWalletSummary();
            incrementBalanceTextField.clear();
            AlertUtils.showInfo("Ví tiền", response == null ? "Cập nhật số dư thành công." : response.getMessage());
        });

        task.setOnFailed(event -> {
            setSubmitting(false);
            Throwable error = task.getException();
            String message = error instanceof ClientServiceException
                    ? error.getMessage()
                    : "Không thể cập nhật số dư.";
            AlertUtils.showError("Ví tiền", message);
        });

        Thread thread = new Thread(task, "wallet-add-balance");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateSessionWallet(BalanceResponse response) {
        if (response == null) {
            return;
        }

        ClientSession.updateWalletSummary(
                response.getBalance(),
                response.getUnpaidWinningAmount(),
                response.getAvailableBalance()
        );
    }

    private void renderWalletSummary() {
        setLabel(currentBalanceLabel, "Balance", ClientSession.getBalance());
        setLabel(availableBalanceLabel, "Available", ClientSession.getAvailableBalance());
        setLabel(unpaidWinningAmountLabel, "Unpaid winning", ClientSession.getUnpaidWinningAmount());
    }

    private void setLabel(Label label, String prefix, Double value) {
        if (label == null) {
            return;
        }

        label.setText(prefix + ": " + (value == null ? "N/A" : FormatUtils.currency(value)));
    }

    private double parsePositiveAmount(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền.");
        }

        try {
            double amount = Double.parseDouble(rawValue.trim());
            if (amount <= 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền phải là số dương.");
        }
    }

    private void setSubmitting(boolean submitting) {
        incrementBalanceButton.setDisable(submitting);
        backButton.setDisable(submitting);
    }
}
