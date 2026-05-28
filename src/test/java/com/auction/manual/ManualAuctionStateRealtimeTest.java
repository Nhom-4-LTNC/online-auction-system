package com.auction.manual;

import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.AuctionUpdateType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.CloseAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.protocol.auth.RegisterRequest;
import com.auction.shared.protocol.bid.PlaceBidRequest;
import com.auction.shared.protocol.event.AuctionUpdatedEvent;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import com.auction.shared.protocol.finance.PayAuctionRequest;

public class ManualAuctionStateRealtimeTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = Integer.getInteger("auction.manual.port", 8888);
    private static final String PASSWORD = "123456";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    public static void main(String[] args) throws Exception {
        System.out.println("========== MANUAL AUCTION STATE REALTIME TEST ==========");
        try (
                ManualClient seller = new ManualClient(HOST, PORT);
                ManualClient bidder = new ManualClient(HOST, PORT)
        ) {
            registerAndLogin(seller, "state_seller_" + RUN_ID, "state_seller_" + RUN_ID + "@test.com");
            registerAndLogin(bidder, "state_bidder_" + RUN_ID, "state_bidder_" + RUN_ID + "@test.com");
            expectSuccess(bidder.send(new Request<>(ActionType.ADD_BALANCE, new AddBalanceRequest(1_000_000))),
                    "Add bidder balance");

            int auctionId = createAuction(seller);
            expectSuccess(bidder.send(new Request<>(ActionType.PLACE_BID, new PlaceBidRequest(auctionId, 200_000))),
                    "Bidder places bid");

            expectError(bidder.send(new Request<>(ActionType.CLOSE_AUCTION, new CloseAuctionRequest(auctionId))),
                    "Non-owner cannot close auction");

            expectSuccess(seller.send(new Request<>(ActionType.CLOSE_AUCTION, new CloseAuctionRequest(auctionId))),
                    "Seller closes auction");
            expectAuctionUpdated(bidder.readResponse(), auctionId, AuctionUpdateType.AUCTION_CLOSED,
                    "Bidder receives AUCTION_CLOSED");

            expectSuccess(bidder.send(new Request<>(ActionType.PAY_AUCTION, new PayAuctionRequest(auctionId))),
                    "Winner pays auction");
            expectAuctionUpdated(seller.readResponse(), auctionId, AuctionUpdateType.PAYMENT_COMPLETED,
                    "Seller receives PAYMENT_COMPLETED");
        }
        System.out.println("========== MANUAL AUCTION STATE REALTIME TEST PASSED ==========");
    }

    private static void registerAndLogin(ManualClient client, String username, String email) throws Exception {
        expectSuccess(client.send(new Request<>(ActionType.REGISTER, new RegisterRequest(username, PASSWORD, email))),
                "Register " + username);
        expectSuccess(client.send(new Request<>(ActionType.LOGIN, new LoginRequest(email, PASSWORD))),
                "Login " + username);
    }

    private static int createAuction(ManualClient seller) throws Exception {
        long now = System.currentTimeMillis();
        ElectronicsDTO item = new ElectronicsDTO(
                "State Realtime Item " + RUN_ID,
                "Manual state realtime item",
                100_000,
                null,
                null,
                "Demo",
                12
        );
        Response<?> response = seller.send(new Request<>(
                ActionType.CREATE_AUCTION,
                new CreateAuctionRequest(item, item.getStartingPrice(), 50_000, now, now + 60L * 60 * 1000)
        ));
        expectSuccess(response, "Seller creates auction");
        CreateAuctionResponse payload = extractPayload(response, CreateAuctionResponse.class);
        return payload.getAuction().getAuctionId();
    }

    private static void expectAuctionUpdated(
            Response<?> response,
            int auctionId,
            AuctionUpdateType updateType,
            String step
    ) {
        if (response == null || response.getAction() != ActionType.AUCTION_UPDATED) {
            throw new AssertionError("[FAIL] " + step + " | expected AUCTION_UPDATED but got "
                    + (response == null ? "null" : response.getAction()));
        }

        AuctionUpdatedEvent event = extractPayload(response, AuctionUpdatedEvent.class);
        if (event.getAuctionId() != auctionId || event.getUpdateType() != updateType) {
            throw new AssertionError("[FAIL] " + step + " | event=" + event);
        }
        if (event.getSummary() == null) {
            throw new AssertionError("[FAIL] " + step + " | missing summary");
        }

        System.out.println("[PASS] " + step + " | summaryStatus=" + event.getSummary().getStatus());
    }

    private static void expectSuccess(Response<?> response, String step) {
        if (response == null || !response.isSuccess()) {
            throw new AssertionError("[FAIL] " + step + " | error="
                    + (response == null ? "null response" : response.getErrorMessage()));
        }
        System.out.println("[PASS] " + step);
    }

    private static void expectError(Response<?> response, String step) {
        if (response == null || response.isSuccess()) {
            throw new AssertionError("[FAIL] " + step + " | expected error");
        }
        System.out.println("[PASS] " + step + " | error=" + response.getErrorMessage());
    }

    private static <T> T extractPayload(Response<?> response, Class<T> expectedType) {
        Object payload = response.getPayload();
        if (!expectedType.isInstance(payload)) {
            throw new AssertionError("Expected payload " + expectedType.getName() + " but got "
                    + (payload == null ? "null" : payload.getClass().getName()));
        }
        return expectedType.cast(payload);
    }
}
