package com.auction.model;

import java.io.Serial;

public class Vehicle extends Item {
    @Serial
    private static final long serialVersionUID = -1051826770648837400L;
    private String brand;
    private String vin;
    private int mileage;
    public Vehicle(int id, String name, String description, Seller owner, double startPrice,
                    String brand, String vin, int mileage) {
        super(id, name, description, owner, startPrice);
        this.brand = brand;
        this.vin = vin;
        this.mileage = mileage;
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

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    @Override
    void displayDetails() {
        System.out.println(toString());
        System.out.println("Vehicle: " + getName() + " - Brand: " + getBrand() +
                " - Identical Number: " + getVin() + " - Travelled: " + getMileage());
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
