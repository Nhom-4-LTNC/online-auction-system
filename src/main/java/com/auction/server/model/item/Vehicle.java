package com.auction.server.model.item;

import com.auction.server.model.user.User;

import java.io.Serial;

public class Vehicle extends Item {
    @Serial
    private static final long serialVersionUID = -1051826770648837400L;
    private String brand;
    private String vin;
    private int mileage;
    public Vehicle (String name, String description, User owner,
                    double startPrice, String imageUrl,
                    String brand, String vin, int mileage) {
        super(name, description, owner, startPrice, imageUrl);
        this.brand = brand;
        this.vin = vin;
        this.mileage = mileage;
    }
    public Vehicle(int id, String name, String description,
                   User owner, double startPrice, String imageUrl,
                   String brand, String vin, int mileage) {
        super(id, name, description, owner, startPrice, imageUrl);
        this.brand = brand;
        this.vin = vin;
        this.mileage = mileage;
    }
    public Vehicle(int id, String name, String description,
                   User owner, double startPrice,
                   String brand, String vin, int mileage) {
        this(id, name, description, owner, startPrice, null, brand, vin, mileage);
    }
    public Vehicle(String name, String description,
                   User owner, double startPrice,
                   String brand, String vin, int mileage) {
        this(name, description, owner, startPrice, null, brand, vin, mileage);
    }
    @Override
    public String getItemType() {
        return "VEHICLE";
    }
    public String getVin() {
        return vin;
    }
    public void setVin(String vin) {
        this.vin = vin;
    }
    public int getMileage() {
        return mileage;
    }
    public void setMileage(int mileage) { this.mileage = mileage;}
    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }

}
