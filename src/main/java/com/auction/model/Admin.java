package com.auction.model;

public class Admin extends User {

    public Admin (String username, String pwd, String email) {
        super(username, pwd, email);
    }
    public Admin(int id, String username, String pwd, String email) {
        super(id, username, pwd, email);
    }

    public void applyBan(User user, long durationInMillis) {
        BanManager banManager = BanManager.getInstance();
        banManager.applyBan(this, user, durationInMillis);
    }

    @Override
    public void displayInfo() {
        System.out.println("placeholder");
    }
}
