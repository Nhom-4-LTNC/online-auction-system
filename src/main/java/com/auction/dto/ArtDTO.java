package com.auction.dto;

import com.auction.model.item.ItemType;

public class ArtDTO extends ItemDTO {
    private final String artist;
    private final int yearCreated;

    public ArtDTO(String name,
                  String description,
                  double startingPrice,
                  byte[] image,
                  String imageFileName,
                  String artist,
                  int yearCreated) {
        super(ItemType.ART, name, description, startingPrice, image, imageFileName);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist() { return artist; }
    public int getYearCreated() { return yearCreated; }
}
