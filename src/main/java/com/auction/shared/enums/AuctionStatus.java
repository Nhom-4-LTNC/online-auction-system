package com.auction.shared.enums;

import java.io.Serializable;

public enum AuctionStatus implements Serializable {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
