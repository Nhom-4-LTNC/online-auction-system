package com.auction.client.session;

import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;

public final class ClientSession {

    private static UserDTO currentUser;

    private ClientSession() {
    }

    public static void setCurrentUser(UserDTO user) {
        currentUser = user;
    }

    public static UserDTO getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isAdmin() {
        return currentUser != null && Role.ADMIN.equals(currentUser.getRole());
    }

    public static Integer getCurrentUserId() {
        return currentUser == null ? null : currentUser.getId();
    }

    public static String getUsername() {
        return currentUser == null ? "" : currentUser.getUsername();
    }
}
