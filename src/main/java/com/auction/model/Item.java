package com.auction.model;

import javax.xml.stream.events.StartDocument;
import java.io.Serial;

public abstract class Item extends Entity {
    @Serial
    private static final long serialVersionUID = 4801033207420599291L;
    private double startPrice;
    private String name;
    private String description;
    private Seller owner;
    public Item(String name, String description,Seller owner, double startPrice) {
        super();
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
    }
    public Item(int id, String name, String description, Seller owner, double startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description;}
    public Integer getOwnerId() { return owner.getId(); }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) {
        if (startPrice > 0) this.startPrice = startPrice;
        else System.out.println("Error! Starting price must be greater than zero!");
    }
    public String toString() {
        return "Item{" +
                "name=" + name + "\n" +
                "description=" + description + "\n" +
                "startPrice= " + startPrice + "\n" +
                "owner=" + owner.getUsername()+ "}";
    }
    abstract void displayDetails();
}
