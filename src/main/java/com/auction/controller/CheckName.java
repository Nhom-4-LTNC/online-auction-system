package com.auction.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class CheckName {
    public static boolean checkName(String username){

        String name = username.trim().replaceAll("\\s++", "");
        int lim = name.length();
        if(name.equals(username)){
            if(3<= lim && lim <=20){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
