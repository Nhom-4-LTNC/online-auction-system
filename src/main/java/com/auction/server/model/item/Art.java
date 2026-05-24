package com.auction.server.model.item;

import com.auction.server.model.user.User;
import com.auction.shared.enums.ItemType;

import java.io.Serial;

public class Art extends Item {
    @Serial
    private static final long serialVersionUID = -1593174092660038685L;
    private String artist;
    private int yearCreated;
    public Art(String name, String description,
               User owner, double startPrice,
               String imageUrl,
               String artist, int yearCreated) {
        super(name, description, owner, startPrice, imageUrl);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
    public Art(int id, String name, String description,
               User owner, double startPrice,
               String imageUrl,
               String artist, int yearCreated) {
        super(id, name, description, owner, startPrice, imageUrl);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
    public Art(int id, String name, String description,
               User owner, double startPrice,
               String artist, int yearCreated) {
        this(id, name, description, owner, startPrice, null, artist, yearCreated);
    }
    public Art(String name, String description,
               User owner, double startPrice,
               String artist, int yearCreated) {
        this(name, description, owner, startPrice, null, artist, yearCreated);
    }
    @Override
    public ItemType getItemType() {
        return ItemType.ART;
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
    public boolean setYearCreated(int yearCreated) {
        if (yearCreated > 0) {
            this.yearCreated = yearCreated;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Art{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", owner=" + getOwner().getUsername() +
                ", startPrice=" + getStartPrice() +
                ", artist='" + artist + '\'' +
                ", yearCreated=" + yearCreated +
                '}';
    }
}
