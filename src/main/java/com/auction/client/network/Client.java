package com.auction.client.network;

import com.auction.shared.network.NetworkConfig;
import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

public class Client {
    private static volatile Client instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessageReceived;
    private final Map<ActionType, BlockingQueue<Response<?>>> pendingResponses = new ConcurrentHashMap<>();

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

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null;
    }

    public void sendMessage(Object message) {
        if (out == null) {
            System.err.println("Loi gui tin nhan toi server: chua ket noi");
            return;
        }
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Loi gui tin nhan toi server: " + e.getMessage());
        }
    }

    public Response<?> sendRequestAndWait(Request<?> request, long timeoutMillis) throws Exception {
        connect();
        if (!isConnected()) {
            throw new IOException("Chua ket noi server");
        }

        BlockingQueue<Response<?>> queue = new ArrayBlockingQueue<>(1);
        pendingResponses.put(request.getAction(), queue);
        try {
            sendMessage(request);
            Response<?> response = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new IOException("Qua thoi gian cho phan hoi tu server");
            }
            return response;
        } finally {
            pendingResponses.remove(request.getAction(), queue);
        }
    }

    private void listenForData() {
        try {
            while (true) {
                Object receivedData = in.readObject();
                if (receivedData instanceof Response<?> response) {
                    BlockingQueue<Response<?>> queue = pendingResponses.get(response.getAction());
                    if (queue != null) {
                        queue.offer(response);
                    }
                }
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
