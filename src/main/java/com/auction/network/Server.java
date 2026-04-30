package com.auction.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    public static int PORT = 8000;

    // Use ObjectOutputStream list for broadcasting objects
    private static final List<ObjectOutputStream> allClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SERVER: Listening on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("SERVER: Connection accepted!");
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    System.out.println("Accept failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
        }
    }

    private static void handleClient(Socket clientSocket) {
        // We define the output stream outside try-with-resources to manage the list manually
        ObjectOutputStream out = null;
        try {
            // 1. Create Output FIRST to match Client's Input
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            allClients.add(out);

            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                // 2. Read the object (This blocks until an object arrives)
                Object receivedData = in.readObject();
                System.out.println("SERVER received: " + receivedData);

                // 3. BROADCAST to everyone
                for (ObjectOutputStream clientOut : allClients) {
                    clientOut.writeObject(receivedData);
                    clientOut.flush();
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected normally.");
        } catch (Exception e) {
            System.out.println("Server handling error: " + e.getMessage());
        } finally {
            // 4. Cleanup
            if (out != null) {
                allClients.remove(out);
            }
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}