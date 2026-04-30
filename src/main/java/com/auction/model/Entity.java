package com.auction.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = -3691692181672595753L;
    protected int id;
    private final LocalDateTime createdAt;
    public Entity() {
        this.createdAt = LocalDateTime.now();
    }
    // CONSTRUCTORS
    public Entity(int id) {
        this();
        this.id = id;
    }

    // GETTERS SETTERS
    public Integer getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Entity)) return false;
        Entity entity = (Entity) obj;
        return id != -1 && id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
