package com.auction.shared.dto;

import com.auction.shared.enums.ItemType;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO (Data Transfer Object) cho Item, dùng để truyền dữ liệu giữa client và server mà không cần phải gửi toàn bộ đối
 * tượng Item.
 * */
public abstract class ItemDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ItemType type;
    private final String name;
    private final String description;
    private final double startingPrice;
    private final byte[] imageData;
    private final String imageFileName;

    public ItemDTO(ItemType type,
                   String name,
                   String description,
                   double startingPrice,
                   byte[] image,
                   String imageFileName) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.imageData = image;
        this.imageFileName = imageFileName;
    }

    public ItemType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public byte[] getImageData() { return imageData; }
    public String getImageFileName() { return imageFileName; }

    @Override
    public String toString() {
        return String.format("ItemDTO{type=%s, name='%s', description='%s', startingPrice=%.2f, imageData=%s, imageFileName='%s'}",
                type, name, description, startingPrice, imageData, imageFileName);
    }
}
