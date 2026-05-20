package com.auction.server;

import com.auction.config.NetworkConfig;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(NetworkConfig.PORT)) {
            System.out.println("Server đang chạy ở: "+NetworkConfig.PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Server: Client kết nối từ IP: "+clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                } catch (IOException e) {
                    System.out.println("Lỗi "+e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khởi động server");
        }
    }
    public static void broadcast(Object data) {
        for(ClientHandler client: clients) {
            client.sendObject(data);
        }
    }
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}