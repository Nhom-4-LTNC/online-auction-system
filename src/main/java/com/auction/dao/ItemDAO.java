package com.auction.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.auction.model.item.Item;

public class ItemDAO {
    private static ItemDAO instance;
    private List<Item> items;
    private static final String FILE_PATH = "items.dat";

    private ItemDAO() {
        items = new ArrayList<>();
        loadData();
    }

    public static synchronized ItemDAO getInstance() {
        if (instance == null) {
            instance = new ItemDAO();
        }
        return instance;
    }

    public synchronized void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            out.writeObject(items);
            System.out.println("Da luu san pham dau gia vao file: "+FILE_PATH);
        } catch (IOException e) {
            System.out.println("Loi khi luu san pham "+e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
                items = (List<Item>)in.readObject();
                System.out.println("Da doc "+items.size()+" tu he thong");
            } catch (Exception e) {
                System.out.println("Loi khi doc file "+e.getMessage());
            }
        }
    }

    public synchronized void addItem(Item item) {
        items.add(item);
        saveData();
    }

    public synchronized void updateItem(Item updateItem) {
        for (int i=0; i< items.size(); i++) {
            if (items.get(i).getId() == updateItem.getId()) {
                items.set(i, updateItem);
                saveData();
                return;
            }
        }
    }

    public synchronized void deleteItem(int itemId) {
        items.removeIf(item ->(item.getId() == itemId));
        saveData();
    }

    public synchronized Item getItemById(int itemId) {
        for (Item item: items) {
            if (item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public synchronized List<Item> getAllItems() {
        return items;
    }

    public synchronized List<Item> getItemsBySeller(Integer sellerId) {
        List<Item> sellerItems = new ArrayList<>();
        for (Item item: items) {
            if (item.getOwnerId() == sellerId) {
                sellerItems.add(item);
            }
        }
        return sellerItems;
    }
}
