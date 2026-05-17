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
            System.out.println("Server dang chay o: "+NetworkConfig.PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Server: Client ket noi tu IP: "+clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
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