package com.auction.network;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import com.auction.exception.InvalidBidException;
import com.auction.model.auction.AuctionManager;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.ItemType;
import com.auction.model.user.BidderProfile;
import com.auction.model.user.User;
import com.auction.model.user.UserManager;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object receivedData = in.readObject();
                if (receivedData instanceof NetworkMessage) {
                    NetworkMessage msg = (NetworkMessage) receivedData;
                    handleMessage(msg);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client ngat ket loi binh thuong");
        } catch (Exception e) {
            System.out.println("Loi trong qua tinh xu li client "+e.getMessage());
        } finally {
            Server.removeClient(this);
            closeConnections();
        }
    }

    private void handleMessage(NetworkMessage msg) {
        switch (msg.getAction()) {
            case PLACE_BID -> handlePlaceBid(msg.getPayload());
            case CREATE_AUCTION -> handleCreateAuction(msg.getPayload());
            case LOGIN -> handleLogin(msg.getPayload());
            case REGISTER -> handleRegister(msg.getPayload());
            default -> {
                System.out.println("Lệnh không được hỗ trợ: " + msg.getAction());
                sendData(new NetworkMessage(ActionType.ERROR, "Lệnh không hợp lệ!"));
            }
        }
    }

    private void handleLogin(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> credentials = (Map<String, String>) payload;
            String email = credentials.get("email");
            String pass = credentials.get("password");

            // Server tự tra cứu Database/RAM
            User user = UserManager.getInstance().getUserByEmail(email);

            if (user != null && user.getPwd().equals(pass)) {
                // Thành công: Đóng gói thông tin cơ bản gửi về Client
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());

                sendData(new NetworkMessage(ActionType.LOGIN_SUCCESS, userInfo));
                System.out.println("User [" + user.getUsername() + "] đã đăng nhập.");
            } else {
                // Thất bại
                sendData(new NetworkMessage(ActionType.LOGIN_FAILED, "Sai Email hoặc Mật khẩu!"));
            }
        }
    }

    private void handleRegister(Object payload) {

    }
    private void handleGetAuctionList(Object payload) {

    }

    private void handlePlaceBid(Object payload) {
        if (payload instanceof BidMessage) {
            BidMessage bidData = (BidMessage) payload;
            try {
                AuctionManager manager = AuctionManager.getInstance();

                User bidder = UserManager.getInstance().getUserById(bidData.getUserId());
                if (bidder == null) {
                    sendData(new NetworkMessage(ActionType.PLACED_BID_FAILED, "Người dùng không tồn tại!"));
                    return;
                }
                manager.processBid(bidData.getAuctionId(), bidder, bidData.getAmount());
                sendData(new NetworkMessage(ActionType.PLACED_BID_SUCCESS, "Đặt giá thành công!"));
            } catch (InvalidBidException e) {
                System.out.println("Client đặt giá sai luật: " + e.getMessage());
                sendData(new NetworkMessage(ActionType.PLACED_BID_FAILED, e.getMessage()));
            } catch (Exception e) {
                System.out.println("Lỗi hệ thống: " + e.getMessage());
                sendData(new NetworkMessage(ActionType.ERROR, "Lỗi hệ thống: " + e.getMessage()));
            }
        }
    }
    private void handleCreateAuction(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map <String, Object> itemData = (Map <String, Object> ) payload;
            try {
                User seller = UserManager.getInstance().getUserById(100);
                ItemType type = ItemType.valueOf(itemData.get("type").toString());
                double bidStep = 10;
                long startTime = System.currentTimeMillis();
                long endTime = startTime + 99900000;

                AuctionManager.getInstance().createAuction(seller, type, itemData, bidStep, startTime, endTime);
                sendData(new NetworkMessage(ActionType.CREATE_AUCTION_SUCCESS, "Tạo phiên đấu giá thành công!"));
            } catch (Exception e) {
                sendData(new NetworkMessage(ActionType.CREATE_AUCTION_FAILED, "Lỗi tạo phiên đấu giá: " + e.getMessage()));
            }
        }
    }
    public void sendData(Object data) {
        try{
            out.writeObject(data);
            out.flush();
            out.reset();
        } catch (Exception e) {
            System.out.println("Lỗi trong quá trình gửi tin: "+ e.getMessage());
        }
    }

    public void closeConnections() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Đã đóng kết nối và giải phóng tài nguyên cho Client.");
        } catch (IOException e) {
            System.out.println("Lỗi khi ngắt kết nối: "+e.getMessage());
        }
    }
}
