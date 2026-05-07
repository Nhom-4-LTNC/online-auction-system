package com.auction.network;

import java.io.Serializable;

public enum ActionType implements Serializable {
    //Request
    LOGIN,
    REGISTER,
    PLACE_BID,
    CREATE_AUCTION,
    GET_AUCTION_LIST,
    //Response
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    PLACED_BID_SUCCESS,
    PLACED_BID_FAILED,
    CREATE_AUCTION_SUCCESS,
    CREATE_AUCTION_FAILED,
    AUCTION_LIST_RESULT,
    //Broadcast
    NEW_BID_PLACED,
    AUCTION_CLOSED,
    //Common
    SUCCESS,
    ERROR
}
