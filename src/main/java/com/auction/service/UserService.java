package com.auction.service;

import com.auction.model.user.User;

public interface UserService {
    User login(String email, String password) throws Exception;
    User register(String username, String email, String password) throws Exception;
    User getUserById(int id) throws Exception;
    User getUserByEmail(String email) throws Exception;
    void updateUser(User user) throws Exception;
}
