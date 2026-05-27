package com.auction.shared.network;

public class NetworkConfig {
    public static final String SERVER_IP = System.getProperty("auction.network.host", "127.0.0.1");
    public static final int PORT = Integer.getInteger("auction.network.port", 8888);
}
