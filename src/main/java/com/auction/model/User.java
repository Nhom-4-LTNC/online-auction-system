package com.auction.model;

public abstract class User extends Entity {
    private static final long serialVersionUID = 2214371618402864005L;
    protected String username;
    protected String pwd;
    protected String email;
    protected Role role;
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
    public void setEmail() { this.email = email; }

    @Override
    public String toString() {
       return "User{" +
               "username=" + username + "\n" +
               "password=" + pwd + "\n" +
               "email=" + email + "\n}";
    }
    abstract void displayInfo();
}
