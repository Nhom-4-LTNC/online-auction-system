package com.auction.model;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = -3691692181672595753L;
    protected Integer id;
    public Entity() {
        id = null;
    }
    public Entity(int id) {
        this.id = id;
    }
    public Integer getId() { return id; }
    public void setId(int id) { this.id = id; }
}
