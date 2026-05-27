package com.auction.shared.protocol.admin;

import java.io.Serial;
import java.io.Serializable;

public class ApplyBanRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int targetUserId;
    private final long durationMillis;

    public ApplyBanRequest(int targetUserId, long durationMillis) {
        this.targetUserId = targetUserId;
        this.durationMillis = durationMillis;
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}

