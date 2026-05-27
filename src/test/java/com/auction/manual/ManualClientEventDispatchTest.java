package com.auction.manual;

import com.auction.client.network.Client;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.protocol.auth.RegisterRequest;
import com.auction.shared.protocol.bid.PlaceBidRequest;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ManualClientEventDispatchTest {
    private static final long TIMEOUT_MILLIS = 10_000L;
    private static final String PASSWORD = "123456";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    public static void main(String[] args) throws Exception {
        startJavaFxToolkit();

        CountDownLatch auctionUpdatedLatch = new CountDownLatch(1);
        Client client = Client.getInstance();
        client.addEventListener(ActionType.AUCTION_UPDATED, response -> {
            System.out.println("[TEST] Received AUCTION_UPDATED: " + response.getPayload());
            auctionUpdatedLatch.countDown();
        });

        String sellerEmail = "event_seller_" + RUN_ID + "@test.com";
        String bidderEmail = "event_bidder_" + RUN_ID + "@test.com";

        registerAndLogin(client, "event_seller_" + RUN_ID, sellerEmail);
        int auctionId = createAuction(client);

        registerAndLogin(client, "event_bidder_" + RUN_ID, bidderEmail);
        expectSuccess(client.sendRequestAndWait(new Request<>(
                ActionType.ADD_BALANCE,
                new AddBalanceRequest(1_000_000)
        ), TIMEOUT_MILLIS), "Add bidder balance");

        Response<?> placeBidResponse = client.sendRequestAndWait(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, 200_000)
        ), TIMEOUT_MILLIS);

        expectSuccess(placeBidResponse, "PLACE_BID response is not blocked by AUCTION_UPDATED");
        if (!auctionUpdatedLatch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("[FAIL] AUCTION_UPDATED was not dispatched to client listener");
        }

        Platform.exit();
        System.out.println("========== MANUAL CLIENT EVENT DISPATCH TEST PASSED ==========");
    }

    private static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch startupLatch = new CountDownLatch(1);
        try {
            Platform.startup(startupLatch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            startupLatch.countDown();
        }
        if (!startupLatch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
    }

    private static void registerAndLogin(Client client, String username, String email) throws Exception {
        expectSuccess(client.sendRequestAndWait(new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(username, PASSWORD, email)
        ), TIMEOUT_MILLIS), "Register " + username);

        expectSuccess(client.sendRequestAndWait(new Request<>(
                ActionType.LOGIN,
                new LoginRequest(email, PASSWORD)
        ), TIMEOUT_MILLIS), "Login " + username);
    }

    private static int createAuction(Client client) throws Exception {
        long now = System.currentTimeMillis();
        ElectronicsDTO item = new ElectronicsDTO(
                "Client Event Item " + RUN_ID,
                "Manual client event dispatch item",
                100_000,
                null,
                null,
                "Demo",
                12
        );

        Response<?> response = client.sendRequestAndWait(new Request<>(
                ActionType.CREATE_AUCTION,
                new CreateAuctionRequest(item, item.getStartingPrice(), 50_000, now, now + 60L * 60 * 1000)
        ), TIMEOUT_MILLIS);

        expectSuccess(response, "Create auction");
        CreateAuctionResponse payload = extractPayload(response, CreateAuctionResponse.class, "Create auction payload");
        return payload.getAuction().getAuctionId();
    }

    private static void expectSuccess(Response<?> response, String step) {
        if (response == null || !response.isSuccess()) {
            throw new AssertionError("[FAIL] " + step + " | error="
                    + (response == null ? "null response" : response.getErrorMessage()));
        }
        System.out.println("[PASS] " + step);
    }

    private static <T> T extractPayload(Response<?> response, Class<T> expectedType, String step) {
        Object payload = response.getPayload();
        if (!expectedType.isInstance(payload)) {
            throw new AssertionError("[FAIL] " + step + " | expected "
                    + expectedType.getName() + " but got "
                    + (payload == null ? "null" : payload.getClass().getName()));
        }
        return expectedType.cast(payload);
    }
}
