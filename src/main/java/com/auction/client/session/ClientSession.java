package com.auction.client.session;

import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.util.SessionManager;

public final class ClientSession {

    private static UserDTO currentUser;
    private static Double balance;
    private static Double unpaidWinningAmount;
    private static Double availableBalance;

    private ClientSession() {
    }

    public static void setCurrentUser(UserDTO user) {
        currentUser = user;
        balance = user == null ? null : user.getBalance();
        unpaidWinningAmount = null;
        availableBalance = null;
        SessionManager.getInstance().setCurrentUser(user);
    }

    public static UserDTO getCurrentUser() {
        if (currentUser == null) {
            currentUser = SessionManager.getInstance().getCurrentUser();
        }
        hydrateWalletFromCurrentUserIfNeeded();
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public static void clear() {
        currentUser = null;
        balance = null;
        unpaidWinningAmount = null;
        availableBalance = null;
        SessionManager.getInstance().logout();
    }

    public static boolean isAdmin() {
        UserDTO user = getCurrentUser();
        return user != null && Role.ADMIN.equals(user.getRole());
    }

    public static Integer getCurrentUserId() {
        UserDTO user = getCurrentUser();
        return user == null ? null : user.getId();
    }

    public static String getUsername() {
        UserDTO user = getCurrentUser();
        return user == null ? "" : user.getUsername();
    }

    public static void updateWalletSummary(double newBalance, double newUnpaidWinningAmount, double newAvailableBalance) {
        balance = newBalance;
        unpaidWinningAmount = newUnpaidWinningAmount;
        availableBalance = newAvailableBalance;
    }

    public static Double getBalance() {
        hydrateWalletFromCurrentUserIfNeeded();
        return balance;
    }

    public static Double getUnpaidWinningAmount() {
        return unpaidWinningAmount;
    }

    public static Double getAvailableBalance() {
        return availableBalance;
    }

    private static void hydrateWalletFromCurrentUserIfNeeded() {
        if (balance == null && currentUser != null) {
            balance = currentUser.getBalance();
        }
    }
}
