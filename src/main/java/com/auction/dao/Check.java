package com.auction.dao;

import java.util.regex.Pattern;

public class Check {

    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean checkName(String username) {
        // 1. Handle null to prevent NullPointerException
        if (username.isEmpty()) {
            System.out.println("Username cannot be null.");
            return false;
        }

        // 2. Check for any whitespace characters anywhere in the string
        // This is more efficient than replacing/trimming and comparing
        if (username.matches(".*\\s.*")) {
            System.out.println("Username cannot contain spaces.");
            return false;
        }

        // 3. Validate length
        int length = username.length();
        if (length >= 3 && length <= 20) {
            return true;
        } else {
            System.out.println("Username must be between 3 and 20 characters.");
            return false;
        }
    }

    public static boolean checkPass(String password) {
        // 1. Check for null or empty to prevent errors
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return false;
        }

        // 2. Perform the match once
        boolean isValid = pattern.matcher(password).matches();

        // 3. Provide feedback if it fails
        if (!isValid) {
            System.out.println("Password does not meet the security requirements.");
        }

        return isValid;
    }
}
