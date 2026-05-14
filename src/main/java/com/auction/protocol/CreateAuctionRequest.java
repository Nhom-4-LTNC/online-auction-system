package com.auction.protocol;

import java.io.Serializable;

public class CreateAuctionRequest implements Serializable {
    private String itemName;
    private String itemDescription;
    private double startingPrice;
}
