package com.auction.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.auction.model.auction.Auction;

public class AuctionDAO {
    private static AuctionDAO instance;
    private List<Auction> auctions;
    private static final String FILE_PATH = "auction.dat";

    private AuctionDAO() {
        auctions = new ArrayList<>();
        loadData();
    }

    public static synchronized AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }

    public synchronized void saveData() {
        try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            out.writeObject(auctions);
            System.out.println("Da luu du lieu dau gia vao file: "+FILE_PATH);
        } catch (IOException e) {
            System.out.println("Loi khi luu du lieu dau gia "+e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
                auctions = (List<Auction>)in.readObject();
                System.out.println("Da doc du lieu dau gia"+auctions.size()+" tu he thong");
            } catch (Exception e) {
                System.out.println("Loi khi doc du lieu dau gia "+e.getMessage());
            }
        }
    }

    public synchronized void addAuction(Auction auction) {
        auctions.add(auction);
        saveData();
    }

    public synchronized void updateAuction(Auction updateAuction) {
        for (int i=0; i<auctions.size(); i++) {
            if (auctions.get(i).getId() == updateAuction.getId()) {
                auctions.set(i, updateAuction);
                saveData();
                return;
            }
        }
    }

    public synchronized Auction geAuctionById(int auctionId) {
        for (Auction auction: auctions) {
            if (auction.getId() == auctionId) {
                return auction;
            }
        }
        return null;
    }

    /* 
    public synchronized List<Auction> getActiveAuctions() {
        return auctions.stream()
                .filter(auction-> "OPENED".equals(auction.getStatus()))
                .collect(Collectors.toList());
    }
    */

    public synchronized List<Auction> getAllAuctions() {
        return auctions;
    }
}
