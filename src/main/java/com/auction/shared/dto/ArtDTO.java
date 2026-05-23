package com.auction.shared.dto;

import com.auction.shared.enums.ItemType;

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

    @Override
    public String toString() {
        return String.format("ArtDTO{artist='%s', yearCreated=%d, %s}", artist, yearCreated, super.toString());
    }


}
