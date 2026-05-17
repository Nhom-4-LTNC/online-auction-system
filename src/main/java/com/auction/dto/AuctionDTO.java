package com.auction.dto;

import com.auction.model.auction.AuctionStatus;

import java.io.Serial;

/**
 * AuctionDTO là một đối tượng chuyển đổi dữ liệu (DTO) được sử dụng để đóng gói
 * và chuyển dữ liệu liên quan đến cuộc đấu giá giữa các tầng khác nhau của ứng dụng.
 * Nó thường chứa các trường như ID cuộc đấu giá, chi tiết mặt hàng,
 * giá khởi điểm, giá cao nhất hiện tại,
 * trạng thái cuộc đấu giá và thông tin khác liên quan đến cuộc đấu giá.
 */
public class AuctionDTO implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final ItemDTO itemDto;
    private final double currentHighestBid;
    private final AuctionStatus status;
    private final long startTimeMillis;
    private final long endTimeMillis;

    public AuctionDTO(int auctionId, ItemDTO itemDto,
                      double currentHighestBid, AuctionStatus status,
                      long startTimeMillis, long endTimeMillis) {
        this.auctionId = auctionId;
        this.itemDto = itemDto;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }

    public int getAuctionId() { return auctionId; }
    public ItemDTO getItemDto() { return itemDto; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public AuctionStatus getStatus() { return status; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
}

