package com.auction.model;

import java.util.*;
/*
    UserManger triển khai Singleton design pattern
    Nhiệm vụ:
    * Dùng một HashMap để lưu trữ User vơi key là ID, giúp tìm kiếm User trong O(1).
    * Quản lí trạng thái: Khi một User đăng nhập sẽ được lưu vào Map này. Khi thoát sẽ bị xóa
    * Cung cấp dữ liệu: Mọi thành phần khác muốn tương tác với User đều phải lấy từ đây
 */
public class UserManager {
    private Map <Integer, User> onlineUsers;
    private static UserManager instance;
    private UserManager() {
        onlineUsers = new HashMap<>();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void addUser(User user) {
        onlineUsers.put(user.getId(), user);
    }

    public User getUserById(int id) {
        return onlineUsers.get(id);
    }
    public Seller getSellerById(int id) {
        User u = onlineUsers.get(id);
        if (u instanceof Seller) return (Seller) u;
        return null;
    }
}
