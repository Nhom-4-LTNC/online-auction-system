package com.auction.dao;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Check {

    /*
    Password requirements:
    - At least 1 digit
    - At least 1 lowercase AND uppercase letter
    - At least 1 special character
    - No whitespaces
     */
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    private static final Pattern hasDigit = Pattern.compile("[0-9]");
    private static final Pattern hasLower = Pattern.compile("[a-z]");
    private static final Pattern hasUpper = Pattern.compile("[A-Z]");
    private static final Pattern hasSpecial = Pattern.compile("[@#$%^&+=!]");
    private static final Pattern isValidLengthNoWhites = Pattern.compile("^\\S{8,}$");

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
    public static ArrayList<Boolean> checkPassRequirements(String password) {
        boolean isDigitOk = hasDigit.matcher(password).find();
        boolean isLowerOk = hasLower.matcher(password).find();
        boolean isUpperOk = hasUpper.matcher(password).find();
        boolean isSpecialOk = hasSpecial.matcher(password).find();
        boolean isLengthOk = isValidLengthNoWhites.matcher(password).matches();

        return new ArrayList<>(List.of(
                isDigitOk,
                isLowerOk,
                isUpperOk,
                isSpecialOk,
                isLengthOk
        ));
    }
    public static boolean checkPass(String password) {
        // 1. Check for null or empty to prevent errors
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return false;
        }


        ArrayList<Boolean> result = checkPassRequirements(password);
        boolean isValid = !result.contains(false);

        if (!isValid) {
            System.out.println("Password does not meet the security requirements.");
        }

        return isValid;
    }
}
