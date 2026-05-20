package com.auction.client.controller;

import java.io.IOException;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.SceneUtils;

import com.auction.client.Client;
import com.auction.protocol.ActionType;
import com.auction.protocol.auction.AuctionResponse;
import com.auction.protocol.auction.SubscribeAuctionRequest;
import com.auction.util.SessionManager;
import com.mysql.cj.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ItemAuctionController {

    private final AuctionService auctionService = AuctionService.getInstance();

    @FXML private Label titleLabel;
    @FXML private Label descLabel;
    @FXML private Label miscLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label endDateLabel;
    @FXML private Label notificationText;

    @FXML private TextField bidTextField;
    @FXML private Button bidButton;


    @FXML
    private void OutAuction(ActionEvent event) throws IOException {
        // Unsubscribe from auction updates before leaving
        try {
            Client client = Client.getInstance();
            if (client.isConnected() && auctionId != null) {
                client.sendMessage(new SubscribeAuctionRequest(auctionId, false));
            }
        } catch (Exception ignore) {}

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

            // Local display for owner of the item (hide bid options)
            bidTextField.setVisible(item.getOwner() != SessionManager.getInstance().getCurrentUser());
            bidButton.setVisible(item.getOwner() != SessionManager.getInstance().getCurrentUser());
            //

            String currentPrice = String.format("%.2f", auction.getCurrentPrice());
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

            // Update UI labels on JavaFX Application Thread
            final String titleText = item.getName();
            final String descText = item.getDescription() == null ? "" : item.getDescription();
            final String miscText = "Owner: " + (item.getOwner() == null ? "Unknown" : item.getOwner().getUsername())
                    + " | Start: " + String.format("%.2f", auction.getStartPrice())
                    + " | Step: " + String.format("%.2f", auction.getBidStep());
            final String priceText = "CURRENT PRICE: " + currentPrice;
            final String endDateText = endsText.isBlank() ? "End: N/A" : "Ends at: " + endsText;

            Platform.runLater(() -> {
                setTitle(titleText);
                if (descLabel != null) descLabel.setText("Description: " + descText);
                if (miscLabel != null) miscLabel.setText(miscText);
                if (currentPriceLabel != null) currentPriceLabel.setText(priceText);
                if (endDateLabel != null) endDateLabel.setText(endDateText);
            });
        } catch (Exception e) {
            Platform.runLater(() -> setTitle(fallbackTitle));
        }

        // Subscribe to live updates for this auction (server will send NOTIFY_NEW_BID messages)
        try {
            Client client = Client.getInstance();
            if (client.isConnected()) {
                // register a message handler for auction notifications
                client.setOnMessageReceived(response -> {
                    if (!(response instanceof AuctionResponse)) return;
                    AuctionResponse ar = (AuctionResponse) response;
                    if (ar.getResponseType() != ActionType.NOTIFY_NEW_BID) return;
                    Auction updated = ar.getAuction();
                    if (updated == null) return;
                    if (updated.getId() != this.auctionId) return; // only care about this item

                    // update UI with latest price and status
                    Platform.runLater(() -> {
                        currentPriceLabel.setText("CURRENT PRICE: " + String.format("%.2f", updated.getCurrentPrice()));
                        setTitle(updated.getItem() == null ? ("AUCTION #" + updated.getId()) : updated.getItem().getName());
                    });
                });

                // send subscribe request
                client.sendMessage(new SubscribeAuctionRequest(auctionId, true));
            }
        } catch (Exception ignore) {
            // ignore subscription errors — view still works with manual refresh
        }
    }


    public void onBidButtonPressed() {
        try {
            // Validate user is logged in
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                notificationText.setText("Please login before bidding!");
                return;
            }

            // Validate bid input
            String bidText = bidTextField.getText().trim();
            if (bidText.isEmpty()) {
                notificationText.setText("Please enter a bid amount!");
                return;
            }

            double tryBidValue = Double.parseDouble(bidText);

            // Fetch fresh auction state
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                notificationText.setText("Auction not found!");
                return;
            }

            // Validate bid amount
            double currentPrice = auction.getCurrentPrice();
            double bidStep = auction.getBidStep();
            double minimumBid = currentPrice + bidStep;

            if (tryBidValue <= currentPrice) {
                notificationText.setText("Bid must be larger than current price (" + currentPrice + ")!");
                return;
            }

            if (tryBidValue < minimumBid) {
                notificationText.setText("Bid must be more than " + minimumBid + " (current + step)!");
                return;
            }

            // Attempt to place bid through service (handles DB update internally)
            auctionService.placeBid(auctionId, currentUser, tryBidValue);

            // Success! Update UI
            Platform.runLater(() -> {
                notificationText.setText("✓ Bid placed successfully!");
                bidTextField.clear();
                // Update current price label
                currentPriceLabel.setText("CURRENT PRICE: " + String.format("%.2f", tryBidValue));
            });

        } catch (com.auction.exception.AuctionClosedException e) {
            notificationText.setText("Auction is closed!");
        } catch (com.auction.exception.InvalidBidException e) {
            notificationText.setText("Invalid bid: " + e.getMessage());
        } catch (com.auction.exception.InsufficientFundsException e) {
            notificationText.setText("Insufficient funds in your account!");
        } catch (NumberFormatException e) {
            notificationText.setText("Please enter a valid number!");
        } catch (Exception e) {
            notificationText.setText("Error placing bid: " + e.getMessage());
        }
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setTitle(String text) {
        if (titleLabel != null) titleLabel.setText(text);
    }

}


