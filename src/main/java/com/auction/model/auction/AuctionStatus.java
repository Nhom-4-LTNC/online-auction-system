package com.auction.model.auction;

import java.io.Serializable;

public enum AuctionStatus implements Serializable {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
