package com.auction.network;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// right now all this does is you can type stuff into the console and it'll send to the server

public class Client {
    public static int SERVER_PORT = Server.PORT;

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", SERVER_PORT)) {
            System.out.println("CLIENT - Connection found.");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Scanner keyboard = new Scanner(System.in);

            // 2. Start a listener thread to handle incoming OBJECTS
            new Thread(() -> {
                try {
                    while (true) {
                        Object received = in.readObject();
                        System.out.println("Received from server: " + received);
                    }
                } catch (EOFException e) {
                    System.out.println("Server closed connection.");
                } catch (Exception e) {
                    System.out.println("Listener error: " + e.getMessage());
                }
            }).start();

            while (keyboard.hasNextLine()) {
                String text = keyboard.nextLine();

                out.writeObject(text);
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}