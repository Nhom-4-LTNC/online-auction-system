package com.auction.shared.protocol.auction;

import com.auction.shared.enums.ItemType;

import java.io.Serial;
import java.io.Serializable;

public class GetAuctionsByTypeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ItemType itemType;
    public GetAuctionsByTypeRequest(ItemType itemType) {
        this.itemType = itemType;
    }

    public ItemType getItemType() {
        return itemType;
    }
}
