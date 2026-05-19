package com.auction.client.controller;

import java.io.IOException;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.service.AuctionService;
import com.auction.util.SceneUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ItemAuctionController {

    private final AuctionService auctionService = AuctionService.getInstance();

    @FXML private Label titleLabel;

    @FXML
    private void OutAuction(ActionEvent event) throws IOException {
        SceneUtils.switchScene(event, "/fxml/AuctionMenu.fxml");
    }

    // --------- Auction detail binding ---------
    private Integer auctionId;

    /**
     * Set auctionId + update title with current product info.
     */
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;

        // Fallback (giữ behavior cũ nếu không fetch được dữ liệu)
        String fallbackTitle = "AUCTION #" + auctionId;

        try {
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                setTitle(fallbackTitle);
                return;
            }

            Item item = auction.getItem();
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                setTitle(fallbackTitle);
                return;
            }

            String category = item.getCategory() == null ? "" : item.getCategory().toString();
            String currentPrice = String.format("%.2f", auction.getCurrentPrice());
            var status = auction.getStatus();
            long endTime = auction.getEndTime();

            String endsText = "";
            try {
                // HH:mm:ss local time (endTime là epoch ms)
                var dt = java.time.Instant.ofEpochMilli(endTime);
                var ldt = java.time.LocalDateTime.ofInstant(dt, java.time.ZoneId.systemDefault());
                endsText = ldt.toLocalTime().toString();
            } catch (Exception ignore) {
                // ignore format time failure
            }

            String formatted = "AUCTION #" + auctionId
                    + " | " + item.getName()
                    + (category.isBlank() ? "" : " (" + category + ")")
                    + " | " + currentPrice
                    + " | " + (status == null ? "" : status)
                    + (endsText.isBlank() ? "" : " | ends " + endsText);

            setTitle(formatted);
        } catch (Exception e) {
            setTitle(fallbackTitle);
        }
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setTitle(String text) {
        if (titleLabel != null) titleLabel.setText(text);
    }

}


