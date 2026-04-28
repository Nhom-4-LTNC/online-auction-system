package com.auction.model;

import java.io.Serial;

public abstract class User extends Entity {
    @Serial
    private static final long serialVersionUID = 2214371618402864005L;

    // BASIC ATTRIBUTES
    protected String username;
    protected String pwd;
    protected String email;
    // BAN ATTRIBUTES
    protected long banStartTime = 0L; // in millis
    protected long banEndTime = 0L;
    // ENUMS
    protected Role role;

    // CONSTRUCTORS
    public User (String username, String pwd, String email) {
        super();
        this.username = username;
        this.pwd = pwd;
        this.email = email;
    }
    public User(int id, String username, String pwd, String email) {
        super(id);
        this.username = username;
        this.pwd = pwd;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getPwd() { return pwd; }
    public String getEmail() { return email; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public void setEmail(String email) { this.email = email; }

    public long getBanStartTime() {return banStartTime;}
    public long getBanEndTime() {return banEndTime;}
    public void setBanStartTime(long banStartTime) { this.banStartTime = banStartTime;}
    public void setBanEndTime(long banEndTime) { this.banEndTime = banEndTime;}

    @Override
    public String toString() {
       return "User{" +
               "username=" + username + "\n" +
               "password=" + pwd + "\n" +
               "email=" + email + "\n}";
    }
    public abstract void displayInfo();
    public abstract void updateRole();
    public Role getRole() {
        return this.role;
    }
}
