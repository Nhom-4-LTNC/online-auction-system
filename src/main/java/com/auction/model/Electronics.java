package com.auction.model;

import java.io.Serial;

public class Electronics extends Item{
    @Serial
    private static final long serialVersionUID = 7901984111386685182L;
    private String brand;
    private int warrantyMonths;
    public Electronics(int id, String name, String description, User owner, double startPrice,
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

    public boolean setWarrantyMonths(int warrantyMonths) {
        if (warrantyMonths > 0) {
            this.warrantyMonths = warrantyMonths;
            return true;
        }
        return false;
    }

}
