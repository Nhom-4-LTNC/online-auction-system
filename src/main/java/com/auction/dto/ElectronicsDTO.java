package com.auction.dto;

import com.auction.model.item.ItemType;

public class ElectronicsDTO extends ItemDTO{
    private final String brand;
    private final int warrantyMonths;

    public ElectronicsDTO(String name,
                          String description,
                          double startingPrice,
                          byte[] image,
                          String imageFileName,
                          String brand,
                          int warrantyMonths) {
        super(ItemType.ELECTRONICS, name, description, startingPrice, image, imageFileName);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public int getWarrantyMonths() { return warrantyMonths; }
}
