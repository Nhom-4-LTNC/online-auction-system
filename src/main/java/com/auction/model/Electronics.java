package com.auction.model;

import java.io.Serial;

public class Electronics extends Item{
    @Serial
    private static final long serialVersionUID = 7901984111386685182L;
    private String brand;
    private int warrantyMonths;
    public Electronics(int id, String name, String description, Seller owner, double startPrice,
                       String brand, int warrantyMonths) {
        super(id, name, description, owner, startPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    void displayDetails() {
        System.out.println(toString());
        System.out.println("Electronic: " + getName() + " - Brand: " + getBrand() +
                " - Warranty remained: " + getWarrantyMonths() + " months");
    }
}
