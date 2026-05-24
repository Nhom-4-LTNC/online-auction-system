package com.auction.server.model.user;

import com.auction.server.model.Entity;
import com.auction.shared.enums.Role;

import java.io.Serial;

public class User extends Entity {
    @Serial
    private static final long serialVersionUID = 2214371618402864005L;

    // BASIC ATTRIBUTES
    private String username;
    private String password;
    private String email;
    private double balance;
    // BAN ATTRIBUTES
    private long banStartTime = 0L; // in millis
    private long banEndTime = 0L;
    // ENUMS
    private Role role;
    // CONSTRUCTORS
    public User(String username, String pwd, String email, Role role) {
        super();
        this.username = username.trim();
        this.password = pwd;
        this.email = email.toLowerCase().trim();
        this.role = role;
    }
    public User(int id, String username, String pwd, String email, Role role) {
        super(id);
        this.username = username;
        this.password = pwd;
        this.email = email;
        this.role = role;
    }
    public boolean isAdmin() { return role == Role.ADMIN; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username;}
    public String getPassword() {
        return password;
    }
    public void setPassword(String pwd) { this.password = pwd; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public long getBanStartTime() {return banStartTime;}
    public long getBanEndTime() {return banEndTime;}
    public void setBanStartTime(long banStartTime) { this.banStartTime = banStartTime;}
    public void setBanEndTime(long banEndTime) { this.banEndTime = banEndTime;}
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

}
