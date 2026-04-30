package com.auction.model.item;

import com.auction.model.user.User;

import java.io.Serial;

public class Art extends Item {
    @Serial
    private static final long serialVersionUID = -1593174092660038685L;
    private String artist;
    private int yearCreated;
    public Art(int id, String name, String description, User owner, double startPrice,
               String artist, int yearCreated) {
        super(id, name, description, owner, startPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
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
}
