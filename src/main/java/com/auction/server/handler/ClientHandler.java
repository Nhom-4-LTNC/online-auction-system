package com.auction.server.handler;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.auction.server.Server;
import com.auction.server.controller.AdminController;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.WalletController;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auth.AuthResponse;

/**
 * Quản lý một kết nối socket từ client.
 *
 * <p>Vai trò chính:
 * <ul>
 *     <li>Giữ session của client hiện tại thông qua {@link #currentUser}</li>
 *     <li>Đọc {@link Request} từ client</li>
 *     <li>Dispatch request tới server controller phù hợp</li>
 *     <li>Gửi {@link Response} về client</li>
 * </ul>
 *
 * <p>ClientHandler không xử lý business logic trực tiếp.
 * Business logic nằm ở Service, được gọi thông qua Server Controller.</p>
 */
public class ClientHandler implements Runnable {

    private volatile boolean closed = false;
    private final Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Session server-side của kết nối hiện tại.
     * Null nghĩa là client chưa đăng nhập hoặc đã logout.
     */
    private UserDTO currentUser;

    private final AuthController authController = new AuthController();
    private final AuctionController auctionController = new AuctionController();
    private final BidController bidController = new BidController();
    private final WalletController walletController = new WalletController();
    private final AdminController adminController = new AdminController();
    private final com.auction.server.controller.AuctionChatController auctionChatController = new com.auction.server.controller.AuctionChatController();


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    @Override
    public void run() {
        try {
            initStreams();
            listenForRequests();

        } catch (EOFException e) {
            System.out.println("[ClientHandler] Client đã ngắt kết nối: " + getClientAddress());

        } catch (IOException e) {
            System.out.println("[ClientHandler] Lỗi I/O với client " + getClientAddress()
                    + ": " + e.getMessage());

        } catch (ClassNotFoundException e) {
            System.out.println("[ClientHandler] Không đọc được object từ client "
                    + getClientAddress() + ": " + e.getMessage());

        } finally {
            closeConnections();
        }
    }

    private void initStreams() throws IOException {
        /*
         * ObjectOutputStream nên được tạo trước ObjectInputStream
         * để tránh deadlock khi cả client và server cùng chờ header stream.
         */
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();

        in = new ObjectInputStream(socket.getInputStream());
    }

    private void listenForRequests() throws IOException, ClassNotFoundException {
        while (!socket.isClosed()) {
            Object incoming = in.readObject();

            if (!(incoming instanceof Request<?> request)) {
                sendError(null, "Gói tin gửi lên không đúng định dạng Request.");
                continue;
            }

            Response<?> response = dispatch(request);

            if (response != null) {
                sendResponse(response);
            }
        }
    }

    private Response<?> dispatch(Request<?> request) {
        ActionType action = request.getAction();

        if (action == null) {
            return Response.error(
                    null,
                    "ActionType không được để trống."
            );
        }
        try {
            return switch (action) {
                // ===== AUTH =====
                case LOGIN -> authController.handleLogin(request, this);

                case REGISTER -> authController.handleRegister(request, this);

                case LOGOUT -> handleLogout();

                // ===== AUCTION =====
                case GET_ALL_AUCTIONS -> auctionController.handleGetAllAuctions();

                case GET_AUCTION -> auctionController.handleGetAuction(request, this);

                case CREATE_AUCTION -> auctionController.handleCreateAuction(request, this);

                case CLOSE_AUCTION -> auctionController.handleCloseAuction(request, this);

                case GET_AUCTIONS_BY_TYPE -> auctionController.handleGetAuctionsByType(request);

                // ===== BID =====
                case PLACE_BID -> bidController.handlePlaceBid(request, this);

                case GET_BIDS_BY_AUCTION -> bidController.handleGetBidsByAuction(request, this);

                case GET_BIDS_BY_BIDDER -> bidController.handleGetBidsByBidder(request, this);

                case GET_MY_BIDS -> bidController.handleGetCurrentUserBids(this);

                // ===== WALLET / PAYMENT =====
                case ADD_BALANCE -> walletController.handleAddBalance(this, request);

                case PAY_AUCTION -> walletController.handlePayAuction(this, request);

                // ===== CHAT (AUCTION) =====
                case SEND_AUCTION_CHAT -> auctionChatController.handleSendChat(request, this);


                // ===== ADMIN =====
                case GET_ALL_USERS -> adminController.handleGetAllUsers(request, this);


                case APPLY_BAN -> adminController.handleApplyBan(request, this);

                case REMOVE_BAN -> adminController.handleRemoveBan(request, this);

                // ===== REALTIME / SERVER PUSH =====
                /*
                 * Các action này thường là server gửi xuống client.
                 * Client không nên gửi chúng lên server.
                 */
                case AUCTION_CREATED, AUCTION_UPDATED, AUCTION_CLOSED -> Response.error(
                        action,
                        "Action này chỉ được server gửi tới client."
                );
                default -> null;
            };

        } catch (ClassCastException e) {
            return Response.error(
                    action,
                    "Payload không đúng kiểu cho action: " + action
            );

        } catch (Exception e) {
            return Response.error(
                    action,
                    e.getMessage()
            );
        }
    }

    private Response<AuthResponse> handleLogout() {
        currentUser = null;

        return Response.success(
                ActionType.LOGOUT,
                new AuthResponse(null, "Đăng xuất thành công.")
        );
    }

    public synchronized void sendResponse(Response<?> response) {
        if (response != null
                && (response.getAction() == ActionType.GET_AUCTIONS_BY_TYPE
                || response.getAction() == ActionType.GET_ALL_AUCTIONS)) {
            System.out.println("[ClientHandler] Sending response action=" + response.getAction()
                    + ", success=" + response.isSuccess());
        }
        sendObject(response);
    }

    /**
     * Dùng cho cả response thường và server-push realtime.
     * Ví dụ: Response<>(ActionType.AUCTION_UPDATED, payload)
     */

    public synchronized void sendObject(Object object) {
        if (object == null) {
            return;
        }

        if (out == null || socket == null || socket.isClosed()) {
            closeConnections();
            return;
        }

        try {
            out.writeObject(object);
            out.flush();
            out.reset();

        } catch (IOException e) {
            System.out.println("[ClientHandler] Lỗi gửi dữ liệu tới client "
                    + getClientAddress() + ": " + e.getMessage());
            closeConnections();
        }
    }

    private void sendError(ActionType action, String message) {
        sendResponse(Response.error(
                action,
                message
        ));
    }

    private String getClientAddress() {
        if (socket == null || socket.getInetAddress() == null) {
            return "unknown";
        }

        return socket.getInetAddress().getHostAddress();
    }

    private void closeConnections() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {}

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}

        Server.removeClient(this);
    }
}
