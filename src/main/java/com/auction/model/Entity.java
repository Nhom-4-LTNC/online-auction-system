package com.auction.model;

import java.io.Serial;
import java.io.Serializable;

public abstract class Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = -3691692181672595753L;

    protected Integer id = null;
    public Entity() {
        this.id = null;
    }
    // CONSTRUCTORS
    public Entity(int id) {
        this.id = id;
    }

    // GETTERS SETTERS
    public Integer getId() { return id; }
    public void setId(int id) { this.id = id; }
}
