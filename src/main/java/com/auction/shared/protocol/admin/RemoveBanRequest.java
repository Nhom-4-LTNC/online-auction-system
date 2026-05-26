package com.auction.shared.protocol.admin;

import java.io.Serial;
import java.io.Serializable;

public class RemoveBanRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int targetUserId;

    public RemoveBanRequest(int targetUserId) {
        this.targetUserId = targetUserId;
    }

    public int getTargetUserId() {
        return targetUserId;
    }
}

