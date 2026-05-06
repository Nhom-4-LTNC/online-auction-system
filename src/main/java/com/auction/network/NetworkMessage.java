package com.auction.network;

import java.io.Serial;
import java.io.Serializable;

public class NetworkMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = -1743328215827377308L;

    private ActionType action;
    private Object payload;

    public NetworkMessage(ActionType action) {
        this.action = action;
        this.payload = null;
    }
    public NetworkMessage(ActionType action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public ActionType getAction() {
        return action;
    }
    public void setAction(ActionType action) {
        this.action = action;
    }

    public Object getPayload() {
        return payload;
    }
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "NetworkMessage{" +
                "action=" + action +
                ", payload=" + (payload != null ? payload.getClass().getSimpleName() : "null") +
                "}";
    }
}
