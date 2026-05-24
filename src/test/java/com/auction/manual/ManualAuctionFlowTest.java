package com.auction.manual;

import com.auction.shared.dto.AuctionDetailDTO;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.ItemType;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

// TODO: Sửa lại import theo package thật trong project của bạn
import com.auction.shared.protocol.auction.*;
import com.auction.shared.protocol.auth.AuthResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.protocol.auth.RegisterRequest;
import com.auction.shared.protocol.bid.PlaceBidRequest;

// Nếu bạn đã thêm chức năng nạp tiền demo
// import com.auction.shared.protocol.finance.AddBalanceRequest;
// import com.auction.shared.dto.BalanceResponse;

import java.time.Instant;
import java.util.List;

/**
 * Manual end-to-end test for the auction system without JavaFX UI.
 *
 * This test talks to the real server through socket Request/Response.
 *
 * It checks:
 * - login/register flow
 * - create auction flow
 * - get auction detail/list flow
 * - place bid flow
 * - invalid bid flow
 * - seller self-bid rejection
 * - close auction flow
 * - bid after close rejection
 *
 * Before running:
 * 1. Start the server first.
 * 2. Make sure HOST and PORT match your Server.
 * 3. Make sure test accounts/passwords are valid or allow registration.
 */
public class ManualAuctionFlowTest {

    private static final String HOST = "127.0.0.1";

    // TODO: đổi port này theo Server của bạn
    private static final int PORT = 8888;

    private static final String PASSWORD = "123456";

    /*
     * Dùng email có timestamp để tránh lỗi register trùng email khi chạy lại test.
     */
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    private static final String SELLER_USERNAME = "seller_" + RUN_ID;
    private static final String SELLER_EMAIL = "seller_" + RUN_ID + "@test.com";

    private static final String BIDDER_USERNAME = "bidder_" + RUN_ID;
    private static final String BIDDER_EMAIL = "bidder_" + RUN_ID + "@test.com";

    private static final String OTHER_BIDDER_USERNAME = "bidder2_" + RUN_ID;
    private static final String OTHER_BIDDER_EMAIL = "bidder2_" + RUN_ID + "@test.com";

    public static void main(String[] args) throws Exception {
        System.out.println("========== MANUAL AUCTION FLOW TEST ==========");
        System.out.println("Server: " + HOST + ":" + PORT);
        System.out.println("Run ID: " + RUN_ID);
        System.out.println();

        try (
                ManualClient sellerClient = new ManualClient(HOST, PORT);
                ManualClient bidderClient = new ManualClient(HOST, PORT);
                ManualClient otherBidderClient = new ManualClient(HOST, PORT)
        ) {
            System.out.println("Connected all manual clients.");

            registerAndLoginSeller(sellerClient);
            registerAndLoginBidder(bidderClient);
            registerAndLoginOtherBidder(otherBidderClient);

            // Nếu project của bạn yêu cầu bidder có balance trước khi bid, bật đoạn này.
            // addBalanceIfSupported(bidderClient, 1_000_000);
            // addBalanceIfSupported(otherBidderClient, 1_000_000);

            int auctionId = createAuctionAsSeller(sellerClient);

            getAuctionDetail(sellerClient, auctionId);
            getAuctionListByType(sellerClient, ItemType.ELECTRONICS);

            sellerCannotBidOwnAuction(sellerClient, auctionId);

            bidderCanPlaceValidBid(bidderClient, auctionId, 120_000);
            bidderCannotPlaceLowBid(otherBidderClient, auctionId, 125_000);

            otherBidderCanPlaceHigherBid(otherBidderClient, auctionId, 140_000);

            nonOwnerCannotCloseAuction(bidderClient, auctionId);
            sellerCanCloseAuction(sellerClient, auctionId);

            bidderCannotBidAfterAuctionClosed(bidderClient, auctionId);

            System.out.println();
            System.out.println("========== ALL MANUAL FLOW TESTS PASSED ==========");
        }
    }

    // =========================================================
    // AUTH FLOW
    // =========================================================

    private static void registerAndLoginSeller(ManualClient sellerClient) throws Exception {
        System.out.println("\n=== AUTH FLOW: SELLER ===");

        Response<?> registerRes = sellerClient.send(new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(SELLER_USERNAME, PASSWORD , SELLER_EMAIL)
        ));

        expectSuccess(registerRes, "Register seller");

        Response<?> loginRes = sellerClient.send(new Request<>(
                ActionType.LOGIN,
                new LoginRequest(SELLER_EMAIL, PASSWORD)
        ));

        expectSuccess(loginRes, "Login seller");

        AuthResponse response = extractPayload(loginRes, AuthResponse.class, "Login seller payload");
        UserDTO seller = response.getUser();
        System.out.println("Seller logged in: id=" + seller.getId() + ", username=" + seller.getUsername());
    }

    private static void registerAndLoginBidder(ManualClient bidderClient) throws Exception {
        System.out.println("\n=== AUTH FLOW: BIDDER ===");

        Response<?> registerRes = bidderClient.send(new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(BIDDER_USERNAME, PASSWORD, BIDDER_EMAIL)
        ));

        expectSuccess(registerRes, "Register bidder");

        Response<?> loginRes = bidderClient.send(new Request<>(
                ActionType.LOGIN,
                new LoginRequest(BIDDER_EMAIL, PASSWORD)
        ));

        expectSuccess(loginRes, "Login bidder");

       AuthResponse response = extractPayload(loginRes, AuthResponse.class, "Login bidder payload");
        UserDTO bidder = response.getUser();
        System.out.println("Bidder logged in: id=" + bidder.getId() + ", username=" + bidder.getUsername());
    }

    private static void registerAndLoginOtherBidder(ManualClient otherBidderClient) throws Exception {
        System.out.println("\n=== AUTH FLOW: OTHER BIDDER ===");
        System.out.println("Register seller:");
        System.out.println("  username=" + SELLER_USERNAME);
        System.out.println("  password=" + PASSWORD);
        System.out.println("  email=" + SELLER_EMAIL);
        Response<?> registerRes = otherBidderClient.send(new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(OTHER_BIDDER_USERNAME, PASSWORD, OTHER_BIDDER_EMAIL)
        ));

        expectSuccess(registerRes, "Register other bidder");

        Response<?> loginRes = otherBidderClient.send(new Request<>(
                ActionType.LOGIN,
                new LoginRequest(OTHER_BIDDER_EMAIL, PASSWORD)
        ));

        expectSuccess(loginRes, "Login other bidder");

        AuthResponse res = extractPayload(loginRes, AuthResponse.class, "Login other bidder payload");
        UserDTO bidder = res.getUser();
        System.out.println("Other bidder logged in: id=" + bidder.getId() + ", username=" + bidder.getUsername());
    }

    // =========================================================
    // AUCTION FLOW
    // =========================================================

    private static int createAuctionAsSeller(ManualClient sellerClient) throws Exception {
        System.out.println("\n=== CREATE AUCTION FLOW ===");

        long endTimeMillis = Instant.now()
                .plusSeconds(60 * 30) // auction kết thúc sau 30 phút
                .toEpochMilli();

        /*
         * TODO: Chỉnh đoạn tạo ElectronicsDTO theo constructor thật của bạn.
         *
         * Ví dụ có thể là:
         *
         * new ElectronicsDTO(
         *     "Laptop Test",
         *     "Laptop for manual socket test",
         *     100_000,
         *     null,
         *     null,
         *     "Dell",
         *     12
         * )
         *
         * Hoặc nếu DTO của bạn khác field, sửa lại cho khớp.
         */
        ElectronicsDTO itemDTO = new ElectronicsDTO(
                "Laptop Test " + RUN_ID,
                "Laptop dùng để test manual auction flow",
                100_000,
                null,
                null,
                "Dell",
                12
        );

        /*
         * TODO: Chỉnh constructor CreateAuctionRequest theo code thật.
         *
         * Ý tưởng request cần có:
         * - itemDTO
         * - bidStep
         * - startingPrice
         * - endTimeMillis
         *
         * Nếu request của bạn có thêm field image/type thì thêm vào.
         */
        long now = System.currentTimeMillis();
        CreateAuctionRequest payload = new CreateAuctionRequest(
                itemDTO,
                itemDTO.getStartingPrice(),
                10_000,
                now,
                now + 60L * 60 * 1000
        );

        Response<?> response = sellerClient.send(new Request<>(
                ActionType.CREATE_AUCTION,
                payload
        ));

        expectSuccess(response, "Seller create auction");

        /*
         * Tùy project của bạn, payload có thể là:
         * - CreateAuctionResponse
         * - AuctionDetailDTO
         * - Integer auctionId
         *
         * Ở đây mình giả định là CreateAuctionResponse.
         */
        Object payloadObj = response.getPayload();

        int auctionId;

        if (payloadObj instanceof CreateAuctionResponse createAuctionResponse) {
            auctionId = createAuctionResponse.getAuction().getAuctionId();
        } else if (payloadObj instanceof AuctionDetailDTO auctionDetailDTO) {
            auctionId = auctionDetailDTO.getAuctionId();
        } else if (payloadObj instanceof Integer id) {
            auctionId = id;
        } else {
            throw new AssertionError(
                    "CREATE_AUCTION payload type is unsupported: " +
                            (payloadObj == null ? "null" : payloadObj.getClass().getName())
            );
        }

        System.out.println("[INFO] Created auctionId=" + auctionId);
        return auctionId;
    }

    private static AuctionDetailDTO getAuctionDetail(ManualClient client, int auctionId) throws Exception {
        System.out.println("\n=== GET AUCTION DETAIL FLOW ===");

        Response<?> response = client.send(new Request<>(
                ActionType.GET_AUCTION,
                new GetAuctionRequest(auctionId)
        ));

        expectSuccess(response, "Get auction detail");

        Object payload = response.getPayload();

        AuctionDetailDTO detail;

        /*
         * Tùy protocol của bạn:
         * - Có thể payload là AuctionDetailDTO trực tiếp
         * - Hoặc là GetAuctionResponse chứa AuctionDetailDTO
         *
         * Ở đây ưu tiên AuctionDetailDTO trực tiếp.
         */
        if (payload instanceof GetAuctionResponse res) {
            detail = res.getAuction();
        } else {
            throw new AssertionError(
                    "GET_AUCTION payload is not AuctionDetailDTO. Actual: " +
                            (payload == null ? "null" : payload.getClass().getName())
            );
        }

        System.out.println("[INFO] Auction detail:");
        System.out.println("  id=" + detail.getAuctionId());
        System.out.println("  item=" + detail.getItem().getName());
        System.out.println("  currentPrice=" + detail.getCurrentPrice());
        System.out.println("  status=" + detail.getStatus());

        return detail;
    }

    private static void getAuctionListByType(ManualClient client, ItemType type) throws Exception {
        System.out.println("\n=== GET AUCTIONS BY TYPE FLOW ===");

        /*
         * Nếu project của bạn chưa có GET_AUCTIONS_BY_TYPE,
         * có thể đổi thành GET_ALL_AUCTIONS rồi filter ở client.
         */
        Response<?> response = client.send(new Request<>(
                ActionType.GET_AUCTIONS_BY_TYPE,
                new GetAuctionsByTypeRequest(type)
        ));

        expectSuccess(response, "Get auctions by type: " + type);

        Object payload = response.getPayload();

        if (!(payload instanceof GetAuctionsByTypeResponse res)) {
            throw new AssertionError(
                    "GET_AUCTIONS_BY_TYPE payload is not List. Actual: " +
                            (payload == null ? "null" : payload.getClass().getName())
            );
        }
        List list = res.getAuctions();
        for (Object obj : list) {
            if (!(obj instanceof AuctionSummaryDTO)) {
                throw new AssertionError("List contains non-AuctionSummaryDTO object: " + obj);
            }
        }

        System.out.println("[INFO] Auction count for " + type + ": " + list.size());
    }

    // =========================================================
    // BID FLOW
    // =========================================================

    private static void sellerCannotBidOwnAuction(ManualClient sellerClient, int auctionId) throws Exception {
        System.out.println("\n=== SELLER SELF-BID FLOW ===");

        Response<?> response = sellerClient.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, 120_000)
        ));

        expectError(response, "Seller cannot bid own auction");
    }

    private static void bidderCanPlaceValidBid(
            ManualClient bidderClient,
            int auctionId,
            double amount
    ) throws Exception {
        System.out.println("\n=== VALID BID FLOW ===");

        Response<?> response = bidderClient.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, amount)
        ));

        expectSuccess(response, "Bidder place valid bid: " + amount);

        System.out.println("[INFO] Valid bid placed: " + amount);
    }

    private static void bidderCannotPlaceLowBid(
            ManualClient bidderClient,
            int auctionId,
            double amount
    ) throws Exception {
        System.out.println("\n=== LOW BID FLOW ===");

        Response<?> response = bidderClient.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, amount)
        ));

        expectError(response, "Bidder cannot place low bid: " + amount);
    }

    private static void otherBidderCanPlaceHigherBid(
            ManualClient otherBidderClient,
            int auctionId,
            double amount
    ) throws Exception {
        System.out.println("\n=== HIGHER BID FLOW ===");

        Response<?> response = otherBidderClient.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, amount)
        ));

        expectSuccess(response, "Other bidder place higher bid: " + amount);

        System.out.println("[INFO] Higher bid placed: " + amount);
    }

    // =========================================================
    // CLOSE AUCTION FLOW
    // =========================================================

    private static void nonOwnerCannotCloseAuction(ManualClient bidderClient, int auctionId) throws Exception {
        System.out.println("\n=== NON-OWNER CLOSE AUCTION FLOW ===");

        Response<?> response = bidderClient.send(new Request<>(
                ActionType.CLOSE_AUCTION,
                new CloseAuctionRequest(auctionId)
        ));

        expectError(response, "Non-owner cannot close auction");
    }

    private static void sellerCanCloseAuction(ManualClient sellerClient, int auctionId) throws Exception {
        System.out.println("\n=== OWNER CLOSE AUCTION FLOW ===");

        Response<?> response = sellerClient.send(new Request<>(
                ActionType.CLOSE_AUCTION,
                new CloseAuctionRequest(auctionId)
        ));
        AuctionDetailDTO auction = getAuctionDetail(sellerClient, auctionId);
        System.out.println("Status after close: " + auction.getStatus());
        System.out.println("Winner after close: " + auction.getLastBidderUsername());

        expectSuccess(response, "Seller close own auction");
    }

    private static void bidderCannotBidAfterAuctionClosed(
            ManualClient bidderClient,
            int auctionId
    ) throws Exception {
        System.out.println("\n=== BID AFTER CLOSE FLOW ===");

        Response<?> response = bidderClient.send(new Request<>(
                ActionType.PLACE_BID,
                new PlaceBidRequest(auctionId, 200_000)
        ));

        expectError(response, "Bidder cannot bid after auction closed");
    }

    // =========================================================
    // OPTIONAL BALANCE FLOW
    // =========================================================

    /*
     * Bật method này nếu project của bạn đã có ADD_BALANCE.
     *
     * private static void addBalanceIfSupported(ManualClient client, double amount) throws Exception {
     *     System.out.println("\n=== ADD BALANCE FLOW ===");
     *
     *     Response<?> response = client.send(new Request<>(
     *             ActionType.ADD_BALANCE,
     *             new AddBalanceRequest(amount)
     *     ));
     *
     *     expectSuccess(response, "Add balance: " + amount);
     * }
     */

    // =========================================================
    // ASSERT HELPERS
    // =========================================================

    private static void expectSuccess(Response<?> response, String step) {
        if (!isSuccess(response)) {
            throw new AssertionError(
                    "[FAIL] " + step +
                            " | expected SUCCESS but got ERROR: " +
                            response.getErrorMessage()
            );
        }

        System.out.println("[PASS] " + step);
    }

    private static void expectError(Response<?> response, String step) {
        if (isSuccess(response)) {
            throw new AssertionError(
                    "[FAIL] " + step +
                            " | expected ERROR but got SUCCESS. Payload: " +
                            response.getPayload()
            );
        }

        System.out.println("[PASS] " + step + " | error=" + response.getErrorMessage());
    }

    private static boolean isSuccess(Response<?> response) {
        if (response == null || response.getStatus() == null) {
            return false;
        }

        return "SUCCESS".equalsIgnoreCase(response.getStatus().name());
    }

    private static <T> T extractPayload(Response<?> response, Class<T> expectedType, String step) {
        Object payload = response.getPayload();

        if (!expectedType.isInstance(payload)) {
            throw new AssertionError(
                    "[FAIL] " + step +
                            " | expected payload type " + expectedType.getName() +
                            " but got " + (payload == null ? "null" : payload.getClass().getName())
            );
        }

        return expectedType.cast(payload);
    }
}