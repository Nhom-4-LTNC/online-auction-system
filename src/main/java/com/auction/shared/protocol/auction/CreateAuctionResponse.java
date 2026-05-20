package com.auction.shared.protocol.auction;

import com.auction.shared.dto.AuctionDetailDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gói tin server gửi về client sau khi xử lý một yêu cầu liên quan đến phiên đấu giá.
 *
 * <p>Trường {@code auction} sẽ là {@code null} khi phản hồi là thất bại.</p>
 */
public class CreateAuctionResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    private final AuctionDetailDTO auction;
    private final String message;

    /**
     * @param auction      phiên đấu giá liên quan; {@code null} nếu thao tác thất bại
     * @param message      thông điệp mô tả kết quả (dùng để hiển thị cho người dùng)
     */
    public CreateAuctionResponse(AuctionDetailDTO auction, String message) {
        this.auction = auction;
        this.message = message;
    }

    public AuctionDetailDTO getAuction()         { return auction; }
    public String getMessage()          { return message; }
}
