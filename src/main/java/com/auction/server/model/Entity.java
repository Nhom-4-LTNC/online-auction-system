package com.auction.server.model;

import java.io.Serial;
import java.io.Serializable;

public abstract class Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = -3691692181672595753L;

    protected int id;
    private final long createdAt;

    public Entity() {
        this.createdAt = System.currentTimeMillis();
    }

    public Entity(int id) {
        this();
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Entity entity)) return false;
        return id > 0 && entity.id > 0 && id == entity.id;
    }

    @Override
    public int hashCode() {
        if (id > 0) {
            return Integer.hashCode(id);
        }
        return System.identityHashCode(this);
    }
}
