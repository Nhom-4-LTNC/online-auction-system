package com.auction.network;

import com.auction.dao.UserDAO;
import com.auction.model.BidTransaction;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionManager;
import com.auction.model.auction.AuctionObserver;
import com.auction.model.user.User;
import com.auction.model.user.UserManager;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("[SYSTEM] Đang tải dữ liệu người dùng từ file...");
        List<User> savedUsers = UserDAO.getInstance().getAllUser();
        for (User u : savedUsers) {
            UserManager.getInstance().addUser(u);
        }
        System.out.println("[SYSTEM] Đã tải thành công " + savedUsers.size() + " người dùng vào RAM.");

        // (Nếu file chưa có ai, anh có thể nhét tạm 1 tài khoản test vào đây)
        if (savedUsers.isEmpty()) {
            UserManager.getInstance().addUser(new User(1, "Test User", "123", "test@gmail.com"));
            System.out.println("[SYSTEM] Đã tạo tài khoản test: test@gmail.com / 123");
        }

        AuctionManager.getInstance().addObserver(new AuctionObserver() {
            @Override
            public void onNewBidPlace(BidTransaction transaction) {
                NetworkMessage msg = new NetworkMessage(ActionType.NEW_BID_PLACED, transaction);
                System.out.println("[BROADCAST] Phát sóng giao dịch mới: " + transaction.getAmount());
                broadcast(msg);
            }

            @Override
            public void onAuctionClosed(Auction auction) {
                NetworkMessage msg = new NetworkMessage(ActionType.AUCTION_CLOSED, auction.getId());
                System.out.println("[BROADCAST] Phiên đấu giá đã đóng: " + auction.getId());
                broadcast(msg);
            }
        });
        try (ServerSocket serverSocket = new ServerSocket(NetworkConfig.PORT)) {
            System.out.println("Server dang chay o: "+NetworkConfig.PORT);
            while (true) {
                try {
                    Socket clienSocket = serverSocket.accept();
                    System.out.println("Server: Client ket noi tu IP: "+clienSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clienSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                } catch (IOException e) {
                    System.out.println("loi "+e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Loi khoi dong server");
        }
    }
    public static void broadcast(Object data) {
        for(ClientHandler client: clients) {
            client.sendData(data);
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}