package com.auction.dto;

import com.auction.model.item.ItemType;

public class VehicleDTO extends ItemDTO{
    private final String brand;
    private final String vin;
    private final int mileage;

    public VehicleDTO(String name, String description, double startingPrice, String brand, String vin, int mileage) {
        super(ItemType.VEHICLE, name, description, startingPrice);
        this.brand = brand;
        this.vin = vin;
        this.mileage = mileage;
    }

    public String getBrand() { return brand; }
    public String getVin() { return vin; }
    public int getMileage() { return mileage; }
}
