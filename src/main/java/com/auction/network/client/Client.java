package com.auction.network.client;

import com.auction.config.NetworkConfig;
import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class Client {
    private static volatile Client instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessageReceived;

    private Client() {}
    public static Client getInstance() {
        if (instance == null) {
            synchronized (Client.class) {
                if (instance == null) instance = new Client();
            }
        }
        return instance;
    }
    public void setOnMessageReceived(Consumer<Object> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void connect() {
        if (socket != null && !socket.isClosed()) {return;}
        try {
            socket = new Socket(NetworkConfig.SERVER_IP, NetworkConfig.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Client da ket noi toi server thanh cong!");

            Thread listenthread = new Thread(this::listenForData);
            listenthread.setDaemon(true);
            listenthread.start();
        } catch (Exception e) {
            System.out.println("Loi ket loi toi server " + e.getMessage());
        }
    }

    public void sendMessage(Object message) {
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Loi gui tin nhan toi server: " + e.getMessage());
        }
    }

    private void listenForData() {
        try {
            while (true) {
                Object receivedData = in.readObject();
                if (onMessageReceived != null) {
                    Platform.runLater(() -> onMessageReceived.accept(receivedData));
                }
            }
        } catch (EOFException e) {
            System.out.println("Server da dong ket noi");
        } catch (Exception e) {
            System.out.println("Server bi ngat ket noi khoi server");
        }  finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}