package com.auction.server;

import com.auction.server.handler.ClientHandler;
import com.auction.shared.network.NetworkConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int port = Integer.getInteger("auction.server.port", NetworkConfig.PORT);

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

        for (ClientHandler client : connectedClients) {
            System.out.println("[Server] Broadcasting client(s): " +
                    data.getClass().getSimpleName());
            if (client.isLoggedIn()) {
                client.sendObject(data);
            }
        }
    }
    public static int getConnectedClientCount() {
        return connectedClients.size();
    }
}