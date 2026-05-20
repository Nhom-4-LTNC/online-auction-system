package com.auction.model.item;

import com.auction.model.user.User;

import java.io.Serial;

public class Electronics extends Item {
    @Serial
    private static final long serialVersionUID = 7901984111386685182L;
    private String brand;
    private int warrantyMonths;
    public Electronics(String name, String des, User owner,
                       double startPrice, String imageUrl,
                       String brand, int warrantyMonths) {
        super(name, des, owner, startPrice, imageUrl);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }
    public Electronics(int id, String name, String description, User owner, double startPrice,
                       String imageUrl, String brand,
                       int warrantyMonths) {
        super(id, name, description, owner, startPrice, imageUrl);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }
    public Electronics(String name, String des, User owner,
                       double startPrice, String brand,
                       int warrantyMonths) {
        this(name, des, owner, startPrice, null, brand, warrantyMonths);
    }
    public Electronics(int id, String name, String description, User owner,
                       double startPrice,
                       String brand, int warrantyMonths) {
        this(id, name, description, owner, startPrice, null, brand, warrantyMonths);
    }
    @Override
    public String getCategory() {
        return "ELECTRONICS";
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

    @Override
    public String toString() {
        return "Electronics{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", owner=" + getOwner().getUsername() +
                ", startPrice=" + getStartPrice() +
                ", brand='" + brand + '\'' +
                ", warrantyMonths=" + warrantyMonths +
                '}';
    }
}
