package com.auction.network;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class Client {
    private static Client instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<NetworkMessage> currentListener;
    public Client() { }

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }
    public void setListener(Consumer <NetworkMessage> listener) {
        this.currentListener = listener;
    }

    public void connect() {
        try {
            if (socket != null && !socket.isClosed()) return;

            socket = new Socket(NetworkConfig.SERVER_IP, NetworkConfig.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Client da ket noi toi server thanh cong!");

            Thread listenthread = new Thread(this::listenForData);
            listenthread.setDaemon(true);
            listenthread.start();

        } catch (Exception e) {
            System.out.println("Loi ket loi toi server "+e.getMessage());
        }
    }

    private void listenForData() {
        try {
            while (true) {
                Object receivedData = in.readObject();

                if (receivedData instanceof NetworkMessage) {
                    NetworkMessage msg = (NetworkMessage) receivedData;

                    if (currentListener != null) {
                        Platform.runLater(() -> {
                            currentListener.accept(msg);
                        });
                    }
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

    public void sendMessage(NetworkMessage message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
                out.reset();
                System.out.println("Client đã gửi: " + message.getAction());
            } else {
                System.out.println("Lỗi: Luồng gửi dữ liệu chưa được khởi tạo!");
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi gửi tin nhẵn lên Server: " + e.getMessage())    ;
        }
    }

    public void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}