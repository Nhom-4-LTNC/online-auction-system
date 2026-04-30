package com.auction.model.user;

public class BanManager {
    // Singleton
    private static BanManager instance;

    // CONSTRUCTOR
    private BanManager() {

    }

    // GETTER
    public static synchronized BanManager getInstance() {
        if (instance == null) {
            instance = new BanManager();
        }
        return instance;
    }

    // METHODS
    public boolean isBanned(User user) {
        return (user.getBanEndTime() > System.currentTimeMillis());
    }


    public void applyBan(Admin admin, User user, long durationInMillis) {
        // admin applies ban to user
        long banStartTime = System.currentTimeMillis();
        long banEndTime = banStartTime + durationInMillis;

        user.setBanStartTime(banStartTime);
        user.setBanEndTime(banEndTime);
    }
}
