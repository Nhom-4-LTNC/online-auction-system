package com.auction.shared.util;

import com.auction.shared.dto.UserDTO;

// Lưu thông tin user đang đăng nhập để các Controller khác có thể dùng chung
public class SessionManager {

    private static SessionManager instance;
    private UserDTO currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }
}
