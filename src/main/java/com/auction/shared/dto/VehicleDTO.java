package com.auction.shared.dto;

import com.auction.shared.enums.ItemType;

public class VehicleDTO extends ItemDTO{
    private final String brand;
    private final String vin;
    private final int mileage;

    public VehicleDTO(String name,
                      String description,
                      double startingPrice,
                      byte[] image,
                      String imageFileName,
                      String brand, String vin, int mileage) {
        super(ItemType.VEHICLE, name, description, startingPrice, image, imageFileName);
        this.brand = brand;
        this.vin = vin;
        this.mileage = mileage;
    }

    public String getBrand() { return brand; }
    public String getVin() { return vin; }
    public int getMileage() { return mileage; }

    @Override
    public String toString() {
        return String.format("VehicleDTO{brand='%s', vin='%s', mileage=%d}", brand, vin, mileage);
    }
}
