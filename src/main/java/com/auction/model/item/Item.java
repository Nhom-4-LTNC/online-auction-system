package com.auction.model.item;

import com.auction.model.Entity;
import com.auction.model.user.User;

import java.io.Serial;

public abstract class Item extends Entity {
    @Serial
    private static final long serialVersionUID = 4801033207420599291L;
    private double startPrice;
    private String name;
    private String description;
    private User owner;
    public Item(String name, String description,User owner, double startPrice) {
        super();
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
    }
    public Item(int id, String name, String description, User owner, double startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
    }
    public String getName() { return name; }
    public boolean isValid() {
        return name != null && !name.isEmpty() && startPrice > 0;
    }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description;}
    public Integer getOwnerId() {
        return (owner != null) ? owner.getId() : -1;
    }
    public double getStartPrice() { return startPrice; }
    public boolean setStartPrice(double startPrice) {
        if (startPrice > 0) {
            this.startPrice = startPrice;
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        String ownerName = (owner != null) ? owner.getUsername() : "None";
        return String.format("Item: %s | Starting price: %.2f | Seller: %s",
                name, startPrice, ownerName);
    }
}
