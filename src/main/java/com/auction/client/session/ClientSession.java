package com.auction.client.session;

import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.util.SessionManager;

public final class ClientSession {

    private static UserDTO currentUser;

    private ClientSession() {
    }

    public static void setCurrentUser(UserDTO user) {
        currentUser = user;
        SessionManager.getInstance().setCurrentUser(user);
    }

    public static UserDTO getCurrentUser() {
        if (currentUser == null) {
            currentUser = SessionManager.getInstance().getCurrentUser();
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public static void clear() {
        currentUser = null;
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
}
