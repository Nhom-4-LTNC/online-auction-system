package com.auction.manual;

import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auction.CloseAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionRequest;
import com.auction.shared.protocol.auction.CreateAuctionResponse;
import com.auction.shared.protocol.auth.AuthResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.protocol.auth.RegisterRequest;
import com.auction.shared.protocol.bid.PlaceBidRequest;
import com.auction.shared.protocol.finance.AddBalanceRequest;
import com.auction.shared.protocol.finance.PayAuctionRequest;

public class ManualWalletFlowTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = Integer.getInteger("auction.manual.port", 8888);
    private static final String PASSWORD = "123456";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    public static void main(String[] args) throws Exception {
        System.out.println("========== MANUAL WALLET FLOW TEST ==========");
        System.out.println("Run ID: " + RUN_ID);

        try (
                ManualClient sellerClient = new ManualClient(HOST, PORT);
                ManualClient userAClient = new ManualClient(HOST, PORT);
                ManualClient userBClient = new ManualClient(HOST, PORT)
        ) {
            registerAndLogin(sellerClient, "seller_wallet_" + RUN_ID, "seller_wallet_" + RUN_ID + "@test.com");
            registerAndLogin(userAClient, "wallet_a_" + RUN_ID, "wallet_a_" + RUN_ID + "@test.com");
            registerAndLogin(userBClient, "wallet_b_" + RUN_ID, "wallet_b_" + RUN_ID + "@test.com");

            addBalance(userAClient, 1_000_000);
            addBalance(userBClient, 2_000_000);

            int auctionX = createAuction(sellerClient, "Wallet X");
            int auctionY = createAuction(sellerClient, "Wallet Y");
            int auctionFinished = createAuction(sellerClient, "Wallet Finished");
            int auctionZ = createAuction(sellerClient, "Wallet Z");

            expectSuccess(placeBid(userAClient, auctionX, 700_000),
                    "Case 2 setup: User A leads auction X at 700000");
            expectError(placeBid(userAClient, auctionY, 400_000),
                    "Case 2: User A cannot reuse committed leading amount on auction Y");

            expectSuccess(placeBid(userAClient, auctionX, 800_000),
                    "Case 3: User A can raise bid on the same auction");

            expectSuccess(placeBid(userBClient, auctionX, 900_000),
                    "Case 1 setup: User B outbids User A on auction X");
            expectSuccess(placeBid(userAClient, auctionY, 400_000),
                    "Case 1: User A available balance opens after being outbid");

            expectSuccess(placeBid(userBClient, auctionY, 500_000),
                    "Setup: User B outbids User A on auction Y");

            expectSuccess(placeBid(userAClient, auctionFinished, 700_000),
                    "Case 4 setup: User A leads auction to be closed");
            expectSuccess(closeAuction(sellerClient, auctionFinished),
                    "Case 4 setup: Seller closes auction, User A has unpaid winning amount");
            expectError(placeBid(userAClient, auctionZ, 400_000),
                    "Case 4: FINISHED unpaid auction reduces available balance");

            PayAuctionResponse payment = payAuction(userAClient, auctionFinished);
            System.out.println("[INFO] Case 5 payment: newBalance=" + payment.getNewBalance()
                    + ", newUnpaid=" + payment.getNewUnpaidWinningAmount()
                    + ", newAvailable=" + payment.getNewAvailableBalance());
            expectSuccess(placeBid(userAClient, auctionZ, 300_000),
                    "Case 5: After PAY_AUCTION, available balance follows reduced real balance");

            System.out.println("========== MANUAL WALLET FLOW TEST PASSED ==========");
        }
    }

    private static UserDTO registerAndLogin(ManualClient client, String username, String email) throws Exception {
        expectSuccess(client.send(new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(username, PASSWORD, email)
        )), "Register " + username);

        Response<?> login = client.send(new Request<>(
                ActionType.LOGIN,
                new LoginRequest(email, PASSWORD)
        ));
        expectSuccess(login, "Login " + username);
        return extractPayload(login, AuthResponse.class, "Login payload").getUser();
    }

    private static BalanceResponse addBalance(ManualClient client, double amount) throws Exception {
        Response<?> response = client.send(new Request<>(
                ActionType.ADD_BALANCE,
                new AddBalanceRequest(amount)
        ));
        expectSuccess(response, "Add balance " + amount);
        return extractPayload(response, BalanceResponse.class, "Add balance payload");
    }

    private static int createAuction(ManualClient client, String name) throws Exception {
        long now = System.currentTimeMillis();
        ElectronicsDTO item = new ElectronicsDTO(
                name + " " + RUN_ID,
                "Manual wallet flow item",
                100_000,
                null,
                null,
                "Demo",
                12
        );
        Response<?> response = client.send(new Request<>(
                ActionType.CREATE_AUCTION,
                new CreateAuctionRequest(item, item.getStartingPrice(), 100_000, now, now + 60L * 60 * 1000)
        ));
        expectSuccess(response, "Create auction " + name);
        CreateAuctionResponse payload = extractPayload(response, CreateAuctionResponse.class, "Create auction payload");
        return payload.getAuction().getAuctionId();
    }

    private static Response<?> placeBid(ManualClient client, int auctionId, double amount) throws Exception {
        return client.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, amount)
        ));
    }

    private static Response<?> closeAuction(ManualClient client, int auctionId) throws Exception {
        return client.send(new Request<>(
                ActionType.CLOSE_AUCTION,
                new CloseAuctionRequest(auctionId)
        ));
    }

    private static PayAuctionResponse payAuction(ManualClient client, int auctionId) throws Exception {
        Response<?> response = client.send(new Request<>(
                ActionType.PAY_AUCTION,
                new PayAuctionRequest(auctionId)
        ));
        expectSuccess(response, "Case 5: Winner pays finished auction");
        return extractPayload(response, PayAuctionResponse.class, "Pay auction payload");
    }

    private static void expectSuccess(Response<?> response, String step) {
        if (!isSuccess(response)) {
            throw new AssertionError("[FAIL] " + step + " | error=" + response.getErrorMessage());
        }
        System.out.println("[PASS] " + step);
    }

    private static void expectError(Response<?> response, String step) {
        if (isSuccess(response)) {
            throw new AssertionError("[FAIL] " + step + " | expected ERROR but got SUCCESS");
        }
        System.out.println("[PASS] " + step + " | error=" + response.getErrorMessage());
    }

    private static boolean isSuccess(Response<?> response) {
        return response != null
                && response.getStatus() != null
                && "SUCCESS".equalsIgnoreCase(response.getStatus().name());
    }

    private static <T> T extractPayload(Response<?> response, Class<T> expectedType, String step) {
        Object payload = response.getPayload();
        if (!expectedType.isInstance(payload)) {
            throw new AssertionError("[FAIL] " + step + " | expected payload type "
                    + expectedType.getName() + " but got "
                    + (payload == null ? "null" : payload.getClass().getName()));
        }
        return expectedType.cast(payload);
    }
}
