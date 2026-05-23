package com.auction.shared.protocol.bid;

import com.auction.shared.dto.BidDTO;

import java.io.Serial;
import java.util.List;

public class GetBidHistoryResponse implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<BidDTO> bidHistory;
    private final String message;

    public GetBidHistoryResponse(List<BidDTO> bidHistory, String message) {
        this.bidHistory = bidHistory;
        this.message = message;
    }

    public List<BidDTO> getBidHistory() {
        return bidHistory;
    }
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("GetBidHistoryResponse{bidHistory=%s, message='%s'}", bidHistory, message);
    }
}
