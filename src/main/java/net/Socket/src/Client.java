package net.Socket.src; // This line fixes the error

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


public class Client {
    private Socket socket;
    private DataOutputStream out;
    private Scanner in;

    // Note: You will need to import your Server class if it's in a different package
    // or define PORT here.
    private static final int PORT = 1234;

    public Client() {
        try {
            // Using a local PORT constant or Server.PORT if accessible
            socket = new Socket("127.0.0.1", PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new Scanner(System.in);
            writeMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMessages() throws IOException {
        String line = "";
        // Logic Fix: Comparing a String to an int (Server.PORT) will always be false.
        // Use a keyword like "exit" to stop the loop instead.
        while (!line.equalsIgnoreCase("exit")) {
            System.out.print("Enter message (type 'exit' to quit): ");
            line = in.nextLine();
            out.writeUTF(line);
        }
        close();
    }

    private void close() throws IOException {
        socket.close();
        out.close();
        in.close();
    }

    public static void main(String[] args) {
        new Client();
    }
}