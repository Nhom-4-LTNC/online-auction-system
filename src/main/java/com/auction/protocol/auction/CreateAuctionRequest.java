package com.auction.protocol.auction;

import com.auction.dto.ItemDTO;
import com.auction.protocol.ActionType;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gói tin gửi từ client lên server để yêu cầu tạo một phiên đấu giá mới.
 *
 * <p>Client đính kèm {@link ItemDTO} (đã chọn đúng kiểu con: ElectronicsDTO, ArtDTO,
 * VehicleDTO) cùng các tham số phiên. Server nhận gói này, uỷ quyền cho
 * {@code AuctionService.createAuction()} xử lý và trả về {@link AuctionResponse}.</p>
 */
public class CreateAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ActionType requestType = ActionType.CREATE_AUCTION;
    private final int sellerId;
    private final ItemDTO itemDto;
    private final double bidStep;
    private final long startTimeMillis;
    private final long endTimeMillis;

    /**
     * @param sellerId        ID của người bán (lấy từ {@code SessionManager})
     * @param itemDto         DTO chứa thông tin sản phẩm muốn đấu giá
     * @param bidStep         bước giá tối thiểu
     * @param startTimeMillis thời điểm bắt đầu phiên (ms epoch)
     * @param endTimeMillis   thời điểm kết thúc phiên (ms epoch)
     */
    public CreateAuctionRequest(int sellerId, ItemDTO itemDto, double bidStep,
                                long startTimeMillis, long endTimeMillis) {
        this.sellerId = sellerId;
        this.itemDto = itemDto;
        this.bidStep = bidStep;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }

    public ActionType getRequestType()  { return requestType; }
    public int getSellerId()            { return sellerId; }
    public ItemDTO getItemDto()         { return itemDto; }
    public double getBidStep()          { return bidStep; }
    public long getStartTimeMillis()    { return startTimeMillis; }
    public long getEndTimeMillis()      { return endTimeMillis; }
}
