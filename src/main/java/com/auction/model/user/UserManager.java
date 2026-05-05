package com.auction.model.user;

import java.util.*;
/*
    UserManger triển khai Singleton design pattern
    Nhiệm vụ:
    * Dùng một HashMap để lưu trữ User vơi key là ID, giúp tìm kiếm User trong O(1).
    * Quản lí trạng thái: Khi một User đăng nhập sẽ được lưu vào Map này. Khi thoát sẽ bị xóa
    * Cung cấp dữ liệu: Mọi thành phần khác muốn tương tác với User đều phải lấy từ đây
 */
public class UserManager {
    private final Map <Integer, User> users;
    private static volatile UserManager instance;
    private UserManager() {
        users = new HashMap<>();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) instance = new UserManager();
            }
        }
        return instance;
    }

    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public User getUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalizedEmail = email.trim();
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(normalizedEmail)) {
                return user;
            }
        }

        return null;
    }

    public User getUserById(int id) {
        User user = users.get(id);
        if (user != null && user.hasRole(Role.SELLER)) return user;
        return null;
    }
}
