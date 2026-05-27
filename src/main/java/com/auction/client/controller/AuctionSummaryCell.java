package com.auction.client.controller;

import java.util.Locale;

import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuctionSummaryCell extends ListCell<AuctionSummaryDTO> {

    private final VBox root = new VBox(4);
    private final Label titleLabel = new Label();
    private final Label priceLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label statusLabel = new Label();

    private final HBox row = new HBox(10);

    public AuctionSummaryCell() {
        root.setPadding(new Insets(6, 8, 6, 8));
        root.getStyleClass().add("auction-summary-cell");

        row.getChildren().addAll(titleLabel, statusLabel);
        VBox.setVgrow(row, Priority.NEVER);

        root.getChildren().addAll(row, priceLabel, timeLabel);

        statusLabel.setMinWidth(110);
    }

    @Override
    protected void updateItem(AuctionSummaryDTO item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        titleLabel.setText(item.getItemName());
        priceLabel.setText(String.format(Locale.US, "Current: %.2f", item.getCurrentPrice()));
        timeLabel.setText("Ends: " + AuctionMenuControllerUtil.formatEndTime(item.getEndTimeMillis()));

        AuctionStatus st = item.getStatus();
        statusLabel.setText(st == null ? "" : st.name());

        setGraphic(root);
    }

    /** Small helper to avoid bringing java.time formatting logic into controller. */
    static class AuctionMenuControllerUtil {
        static String formatEndTime(long epochMillis) {
            // Keep it simple: show milliseconds if time conversion fails.
            try {
                return java.time.Instant.ofEpochMilli(epochMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                        .toString();
            } catch (Exception e) {
                return String.valueOf(epochMillis);
            }
        }
    }
}

