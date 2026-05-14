package com.auction.protocol.auction;

import com.auction.model.auction.Auction;
import com.auction.protocol.ActionType;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gói tin server gửi về client sau khi xử lý một yêu cầu liên quan đến phiên đấu giá.
 *
 * <p>Dùng cho các action: {@link ActionType#CREATE_AUCTION_SUCCESS},
 * {@link ActionType#CREATE_AUCTION_FAILURE}, {@link ActionType#PLACE_BID_SUCCESS},
 * {@link ActionType#PLACE_BID_FAILURE}, {@link ActionType#AUCTION_CLOSED}.</p>
 *
 * <p>Trường {@code auction} sẽ là {@code null} khi phản hồi là thất bại.</p>
 */
public class AuctionResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    private final ActionType responseType;
    private final Auction auction;
    private final String message;

    /**
     * @param responseType loại phản hồi (SUCCESS / FAILURE / ...)
     * @param auction      phiên đấu giá liên quan; {@code null} nếu thao tác thất bại
     * @param message      thông điệp mô tả kết quả (dùng để hiển thị cho người dùng)
     */
    public AuctionResponse(ActionType responseType, Auction auction, String message) {
        this.responseType = responseType;
        this.auction = auction;
        this.message = message;
    }

    public ActionType getResponseType() { return responseType; }
    public Auction getAuction()         { return auction; }
    public String getMessage()          { return message; }
}
