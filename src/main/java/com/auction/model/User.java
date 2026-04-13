package com.auction.model;

public abstract class User extends Entity {
    protected String username;
    protected String pwd;
    protected String email;
    protected Role role;
    private static int idCounter = 0;
    public User (String username, String pwd, String email) {
        super("UID" + ++idCounter);
        this.username = username;
        this.pwd = pwd;
        this.email = email;
    }
}
