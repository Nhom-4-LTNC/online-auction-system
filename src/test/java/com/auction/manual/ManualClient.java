package com.auction.manual;

import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ManualClientTest implements AutoCloseable{
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ManualClientTest(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public Response<?> send(Request<?> request) throws Exception {
        out.writeObject(request);
        out.flush();

        Object obj = in.readObject();

        if (!(obj instanceof Response<?> response)) {
            throw new IllegalStateException("Incorrect object received");
        }

        return response;
    }

    @Override
    public void close() throws Exception {
        try {
            in.close();
        } finally {
            try {
                out.close();
            } finally {
                socket.close();
            }
        }
    }
}
