package com.auction.shared.protocol;

import java.io.Serializable;

public enum ActionType implements Serializable {
    LOGIN,
    REGISTER,
    LOGOUT,

    CREATE_AUCTION,
    GET_ALL_AUCTIONS,
    GET_AUCTION,
    CLOSE_AUCTION,

    PLACE_BID,
    GET_BIDS_BY_AUCTION,
    GET_BIDS_BY_BIDDER,
    GET_MY_BIDS,

    AUCTION_CREATED,
    AUCTION_CLOSED,
    AUCTION_UPDATED,
}
