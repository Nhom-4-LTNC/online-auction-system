package com.auction.model;

import java.io.Serial;

public class Art extends Item{
    @Serial
    private static final long serialVersionUID = -1593174092660038685L;
    private String artist;
    private int yearCreated;
    public Art(int id, String name, String description, Seller owner, double startPrice,
               String artist, int yearCreated) {
        super(id, name, description, owner, startPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    @Override
    void displayDetails() {
        System.out.println(toString());
        System.out.println("Art: " + getName() + " - Artis: " + getArtist() +
                " - Created year: " + getYearCreated());
    }
    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public int getYearCreated() {
        return yearCreated;
    }
    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }
}
