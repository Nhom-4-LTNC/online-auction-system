package com.auction.server.model.user;

import java.io.Serial;

public class Admin extends User {

    @Serial
    private static final long serialVersionUID = 6101409318491197947L;
    public Admin(String username, String pwd, String email) {
        super(username, pwd, email, Role.ADMIN);
    }

    public Admin(int id, String username, String pwd, String email) {
        super(id, username, pwd, email, Role.ADMIN);
    }
}

