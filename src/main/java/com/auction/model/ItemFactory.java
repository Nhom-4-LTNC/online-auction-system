package com.auction.model;

import java.util.*;
/*
    Lớp ItemFactory triển khai Simple Factory design pattern
    * Nhiệm vụ:
        - Khởi tạo tập trung các lớp con của Item (Electronics, Art, Vehicle)
 */
public class ItemFactory {
    public static Item createItem(ItemType type, Map<String, Object> data) {
        try {
            int id = (data.get("id") != null) ? (Integer) data.get("id") : 0;
            String name = (String) data.get("name");

            double price = Double.parseDouble(data.get("price").toString());

            String desc = (String) data.get("desc");
            Seller owner = (Seller) data.get("owner");

            return switch (type) {
                case ELECTRONICS -> new Electronics(id, name, desc, owner, price,
                        (String) data.get("brand"),
                        (int) data.get("warranty"));

                case ART -> new Art(id, name, desc, owner, price,
                        (String) data.get("artist"),
                        (int) data.get("year"));

                case VEHICLE -> new Vehicle(id, name, desc, owner, price,
                        (String) data.get("brand"),
                        (String) data.get("vin"),
                        (int) data.get("mileage"));

                default -> throw new IllegalArgumentException("Unknown ItemType");
            };
        } catch (Exception e) {
            System.err.println("Create Item Error: " + e.getMessage());
            return null;
        }
    }
}
