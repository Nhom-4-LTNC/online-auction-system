package com.auction.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 4801033207420599291L;

    private double startPrice;
    private String name;
    private String description;
    private Seller owner;

    // CONSTRUCTORS
    public Item(String name, String description, double startPrice) {
        super();
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
    }
    public Item(int id, String name, String description, double startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
    }


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description;}

    public Seller getOwnerId() { return owner; }
}
