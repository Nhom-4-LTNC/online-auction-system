package com.auction.network;

import java.io.*;
import java.net.*;

import com.auction.exception.InvalidBidException;
import com.auction.model.auction.AuctionManager;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object receivedData = in.readObject();
                if (isValidBid(receivedData)) {
                    Server.broadcast(receivedData);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client ngat ket loi binh thuong");
        } catch (Exception e) {
            System.out.println("Loi trong qua tinh xu li client "+e.getMessage());
        } finally {
            Server.removeClient(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isValidBid(Object receivedData) {
        if (receivedData instanceof BidMessage) {
            BidMessage bidData = (BidMessage) receivedData;
            try {
                AuctionManager manager = AuctionManager.getInstance();
                manager.processBid(
                    bidData.getAuctionId(),
                    bidData.getUser(),
                    bidData.getAmount()
                );
                return true;
            } catch (InvalidBidException e) {
                System.out.println("Client dat gia sai luat: "+e.getMessage());
                this.sendData("Loi: "+e.getMessage());
                return false;
            } catch (Exception e) {
                System.out.println("Loi he thong: "+e.getMessage());
                this.sendData("Loi he thong: "+e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public void sendData(Object data) {
        try{
            out.writeObject(data);
            out.flush();
            out.reset();
        } catch (Exception e) {
            System.out.println("Loig trong qua trinh gui tin.");
        }
    }
}
