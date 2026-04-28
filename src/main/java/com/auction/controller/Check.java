package com.auction.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Check {

    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean checkName(String username){
        String name = username.trim().replaceAll("\\s++", "");
        int lim = name.length();
        if(name.equals(username)) {
            if (3 <= lim && lim <= 20) return true;
            else return false;
        }else {
            return false;
        }
    }

    public static boolean checkPass(String password){
        if(password.isEmpty()){
            return false;
        }
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}
