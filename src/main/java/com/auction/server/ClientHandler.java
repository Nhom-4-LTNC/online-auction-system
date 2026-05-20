package com.auction.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.auction.model.user.User;
import com.auction.protocol.ActionType;
import com.auction.protocol.AuthRequest;
import com.auction.protocol.AuthResponse;
import com.auction.protocol.BidMessage;
import com.auction.protocol.auction.AuctionResponse;
import com.auction.protocol.auction.CreateAuctionRequest;
import com.auction.service.AuctionService;
import com.auction.service.UserService;

public class ClientHandler implements Runnable {


    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final java.util.Set<Integer> subscribedAuctions = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public boolean isSubscribedToAuction(int auctionId) {
        return subscribedAuctions.contains(auctionId);
    }

    // ----------------------------------------------------------------
    // VÒNG LẶP NHẬN TIN NHẮN
    // ----------------------------------------------------------------

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object data = in.readObject();

                if (data instanceof AuthRequest authRequest) {
                    handleAuthRequest(authRequest);
                } else if (data instanceof com.auction.protocol.auction.SubscribeAuctionRequest subReq) {
                    handleSubscribeRequest(subReq);
                } else if (data instanceof CreateAuctionRequest createReq) {
                    handleCreateAuctionRequest(createReq);
                } else if (data instanceof BidMessage bidMessage) {
                    handleBidRequest(bidMessage);
                }

            }
        } catch (EOFException e) {
            System.out.println("Client ngắt kết nối bình thường");
        } catch (Exception e) {
            System.out.println("Lỗi xử lý client: " + e.getMessage());
        } finally {
            Server.removeClient(this);
            closeConnections();
        }
    }

    private void handleSubscribeRequest(com.auction.protocol.auction.SubscribeAuctionRequest req) {
        if (req.isSubscribe()) {
            subscribedAuctions.add(req.getAuctionId());
            // Optionally send current auction state
            try {
                AuctionService service = AuctionService.getInstance();
                var auction = service.getAuctionById(req.getAuctionId());
                sendData(new com.auction.protocol.auction.AuctionResponse(com.auction.protocol.ActionType.SUCCESS, auction, "Subscribed"));
            } catch (Exception e) {
                sendData(new com.auction.protocol.auction.AuctionResponse(com.auction.protocol.ActionType.ERROR, null, "Subscribe failed: " + e.getMessage()));
            }
        } else {
            subscribedAuctions.remove(req.getAuctionId());
            sendData(new com.auction.protocol.auction.AuctionResponse(com.auction.protocol.ActionType.SUCCESS, null, "Unsubscribed"));
        }
    }

    // XỬ LÝ ĐĂNG NHẬP / ĐĂNG KÝ
    private void handleAuthRequest(AuthRequest request) {
        if (request.getRequestType() == ActionType.LOGIN) {
            try {
               User user = UserService.getInstance().login(request.getEmail(), request.getPassword());
               user.setPwd(null); // Không gửi mật khẩu về client
               sendData(new AuthResponse(ActionType.LOGIN_SUCCESS, user,"Đăng nhập thành công!"));
            } catch (Exception e) {
                System.out.println("Đăng nhập thất bại: " + e.getMessage());
                sendData(new AuthResponse(ActionType.LOGIN_FAILURE, null, "Đăng nhập thất bại: " + e.getMessage()));
            }

        } else if (request.getRequestType() == ActionType.REGISTER) {
            try {
                // register() ném Exception khi trùng email/username → bắt để gửi FAILURE
                User user = UserService.getInstance().register(
                        request.getUsername(), request.getEmail(), request.getPassword());
                user.setPwd(null); // Không gửi mật khẩu về client
                sendData(new AuthResponse(ActionType.REGISTER_SUCCESS, user, "Đăng ký thành công!"));
            } catch (Exception e) {
                System.out.println("Đăng ký thất bại: " + e.getMessage());
                sendData(new AuthResponse(ActionType.REGISTER_FAILURE, null, "Đăng ký thất bại: " + e.getMessage()));
            }
        }
    }

    // XỬ LÝ TẠO PHIÊN ĐẤU GIÁ
    private void handleCreateAuctionRequest(CreateAuctionRequest request) {
        try {
            User seller = UserService.getInstance().getUserById(request.getSellerId());
            AuctionService auctionService = AuctionService.getInstance();

            var auction = auctionService.createAuction(
                    seller,
                    request.getItemDto(),
                    request.getBidStep(),
                    request.getStartTimeMillis(),
                    request.getEndTimeMillis()
            );

            sendData(new AuctionResponse(ActionType.CREATE_AUCTION_SUCCESS, auction,
                    "Tạo phiên thành công!"));
        } catch (Exception e) {
            System.out.println("Tạo phiên thất bại: " + e.getMessage());
            sendData(new AuctionResponse(ActionType.CREATE_AUCTION_FAILURE, null, e.getMessage()));
        }
    }

    // XỬ LÝ ĐẶT GIÁ
    private void handleBidRequest(BidMessage bidData) {

        try {
            User bidder = UserService.getInstance().getUserById(bidData.getUserId());
            AuctionService.getInstance().placeBid(bidData.getAuctionId(), bidder, bidData.getAmount());
            // Thông báo cho clients đang subscribe phiên đấu giá này
            try {
                var updated = AuctionService.getInstance().getAuctionById(bidData.getAuctionId());
                com.auction.protocol.auction.AuctionResponse notify = new com.auction.protocol.auction.AuctionResponse(
                        com.auction.protocol.ActionType.NOTIFY_NEW_BID,
                        updated,
                        "New bid placed"
                );
                Server.broadcastToAuction(bidData.getAuctionId(), notify);
            } catch (Exception ex) {
                System.out.println("Broadcast failed to prepare updated auction: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Đặt giá thất bại: " + e.getMessage());
            sendData("Lỗi: " + e.getMessage());
        }
    }
    // GỬI DỮ LIỆU / ĐÓNG KẾT NỐI
    public void sendData(Object data) {
        try {
            out.writeObject(data);
            out.flush();
            out.reset();
        } catch (Exception e) {
            System.out.println("Lỗi gửi tin nhắn: " + e.getMessage());
        }
    }

    public void closeConnections() {
        try {
            if (in != null)     in.close();
            if (out != null)    out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Đã đóng kết nối client.");
        } catch (IOException e) {
            System.out.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }
}
