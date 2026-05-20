package com.auction.client;

import com.auction.client.controller.HomeMenuController;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(HomeMenuController.class, args);
    }
}
