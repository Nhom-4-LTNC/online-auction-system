package com.auction.server;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.handler.ClientHandler;
import com.auction.server.scheduler.AuctionStatusScheduler;
import com.auction.server.service.*;
import com.auction.shared.network.NetworkConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private static void warmUpApplication() {
        System.out.println("[Server] Khởi động application...");

        AuthService.getInstance();
        AuctionService.getInstance();
        UserService.getInstance();
        ItemService.getInstance();
        BidService.getInstance();
        PaymentService.getInstance();
        WalletService.getInstance();

        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("[Server] Khởi động Database OK.");
        } catch (SQLException e) {
            System.err.println("[Server] Lỗi khởi động Database: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("[Server] Hoàn tất khởi động.");
    }
    public static void main(String[] args) {

        int port = Integer.getInteger("auction.server.port", NetworkConfig.PORT);

        warmUpApplication();
        AuctionStatusScheduler.getInstance().start();
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> AuctionStatusScheduler.getInstance().stop(),
                "auction-status-scheduler-shutdown"
        ));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Đang chạy ở port: " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] Client kết nối từ IP: "
                            + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    addClient(handler);

                    Thread clientThread = new Thread(handler);
                    clientThread.start();

                } catch (IOException e) {
                    System.out.println("[Server] Lỗi khi chấp nhận client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] Lỗi khởi động server: " + e.getMessage());
        }
    }

    public static void addClient(ClientHandler client) {
        if (client == null) {
            return;
        }

        connectedClients.add(client);
        System.out.println("[Server] Connected clients: " + connectedClients.size());
    }

    public static void removeClient(ClientHandler client) {
        if (client == null) {
            return;
        }

        boolean removed = connectedClients.remove(client);

        if (removed) {
            System.out.println("[Server] Client disconnected. Connected clients: " + connectedClients.size());
        }
    }

    public static void broadcast(Object data) {
        if (data == null) {
            return;
        }

        System.out.println("[Server] Broadcasting to " + connectedClients.size() + " client(s): "
                + data.getClass().getSimpleName());
        for (ClientHandler client : connectedClients) {
            client.sendObject(data);
        }
    }
    public static void broadcastToLoggedIn(Object data) {
        if (data == null) {
            return;
        }

        int sent = 0;
        for (ClientHandler client : connectedClients) {
            if (!client.isLoggedIn()) {
                continue;
            }

            try {
                client.sendObject(data);
                sent++;
            } catch (Exception e) {
                System.err.println("[Server] Broadcast failed for one client: " + e.getMessage());
            }
        }
        System.out.println("[Server] Broadcast " + data.getClass().getSimpleName()
                + " to logged-in clients: " + sent);
    }

    public static void broadcastToLoggedInExcept(Object data, ClientHandler excludedClient) {
        if (data == null) {
            return;
        }

        int sent = 0;
        for (ClientHandler client : connectedClients) {
            if (client == excludedClient || !client.isLoggedIn()) {
                continue;
            }

            try {
                client.sendObject(data);
                sent++;
            } catch (Exception e) {
                System.err.println("[Server] Broadcast failed for one client: " + e.getMessage());
            }
        }
        System.out.println("[Server] Broadcast " + data.getClass().getSimpleName()
                + " to logged-in clients except requester: " + sent);
    }
    public static int getConnectedClientCount() {
        return connectedClients.size();
    }
}
