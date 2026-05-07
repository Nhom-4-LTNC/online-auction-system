package com.auction.model.user;

import com.auction.model.Entity;

import java.io.Serial;
import java.util.HashSet;
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
    private final Set <Role> roles = new HashSet<>();

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
    public void addRole(Role role) { roles.add(role);}
    public boolean hasRole(Role role) { return roles.contains(role); }

    public synchronized BidderProfile getBidderProfile() {
        if (this.bidderProfile == null) {
            this.bidderProfile = new BidderProfile();
            this.addRole(Role.BIDDER);
        }
        return this.bidderProfile;
    }

    public synchronized SellerProfile getSellerProfile() {
        if (this.sellerProfile == null ) {
            this.sellerProfile = new SellerProfile();
            this.addRole(Role.SELLER);
        }
        return this.sellerProfile;
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
               "email=" + email + "\n}";
    }
}