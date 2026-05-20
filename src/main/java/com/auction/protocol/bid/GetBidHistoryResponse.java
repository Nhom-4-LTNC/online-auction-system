package com.auction.protocol.bid;

import com.auction.dto.BidDTO;

import java.io.Serial;
import java.util.List;

public class GetBidHistoryResponse implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private List<BidDTO> bidHistory;
    private String message;

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
}
