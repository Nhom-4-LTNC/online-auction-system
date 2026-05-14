package com.auction.dto;

import com.auction.model.item.ItemType;

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

    public ItemDTO(ItemType type, String name, String description, double startingPrice) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public ItemType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
}
