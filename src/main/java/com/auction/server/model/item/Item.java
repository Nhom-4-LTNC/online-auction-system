package com.auction.server.model.item;

import com.auction.server.model.Entity;
import com.auction.server.model.user.User;
import com.auction.shared.enums.ItemType;

import java.io.Serial;
import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = 4801033207420599291L;
    private double startPrice;
    private String name;
    private String description;
    private final User owner;
    private String imageUrl;


    public Item(String name, String description,
                User owner, double startPrice, String imageUrl) {
        super();
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
        this.imageUrl = imageUrl;
    }
    public Item(int id, String name, String description,
                User owner, double startPrice, String imageUrl) {
        super(id);
        this.name = name;
        this.description = description;
        if (startPrice > 0) this.startPrice = startPrice;
        this.owner = owner;
        this.imageUrl = imageUrl;
    }
    public Item(int id, String name, String description, User owner, double startPrice) {
        this(id, name, description, owner, startPrice, null);
    }
    public Item(String name, String description, User owner, double startPrice) {
        this(name, description, owner, startPrice, null);
    }
    public boolean isValid() {
        return name != null && !name.isEmpty() && description != null && !description.isEmpty() && owner != null && startPrice > 0;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl;}
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description;}
    public User getOwner() {
        return owner;
    }
    public abstract ItemType getItemType();
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
}
