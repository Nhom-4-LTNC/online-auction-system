package com.auction.model;

import java.io.Serial;
import java.util.Set;

public class User extends Entity {
    @Serial
    private static final long serialVersionUID = 2214371618402864005L;

    // BASIC ATTRIBUTES
    private final String username;
    private String pwd;
    private String email;
    // BAN ATTRIBUTES
    private long banStartTime = 0L; // in millis
    private long banEndTime = 0L;
    // ENUMS
    private Set <Role> roles;

    private BidderProfile bidderProfile;
    private SellerProfile sellerProfile;

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
    public void addRole(Role role) {
        roles.add(role);
        if (role == Role.BIDDER && bidderProfile == null) {
            bidderProfile = new BidderProfile();
        }
        if (role == Role.SELLER && sellerProfile == null) {
            sellerProfile = new SellerProfile();
        }
    }
    public boolean hasRole(Role role) { return roles.contains(role); }
    public BidderProfile getBidderProfile() { return bidderProfile; }
    public SellerProfile getSellerProfile() { return sellerProfile; }
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
               "email=" + email + "\n}";
    }
}
