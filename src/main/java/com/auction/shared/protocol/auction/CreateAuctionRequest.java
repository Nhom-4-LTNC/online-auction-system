package com.auction.shared.protocol.auction;

import com.auction.shared.dto.ItemDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * Gói tin gửi từ client lên server để yêu cầu tạo một phiên đấu giá mới.
 *
 * <p>Client đính kèm {@link ItemDTO} (đã chọn đúng kiểu con: ElectronicsDTO, ArtDTO,
 * VehicleDTO) cùng các tham số phiên. Server nhận gói này, uỷ quyền cho
 * {@code AuctionService.createAuction()} xử lý và trả về {@link CreateAuctionResponse}.</p>
 */
public class CreateAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ItemDTO itemDto;
    private final double startingPrice;
    private final double bidStep;
    private final long startTime;
    private final long endTime;

    /**
     * @param itemDto         DTO chứa thông tin sản phẩm muốn đấu giá
     * @param bidStep         bước giá tối thiểu
     * @param startTime       thời điểm bắt đầu phiên (ms epoch)
     * @param endTime         thời điểm kết thúc phiên (ms epoch)
     */
    public CreateAuctionRequest(ItemDTO itemDto, double startingPrice, double bidStep,
                                              long startTime, long endTime) {
        this.itemDto = itemDto;
        this.startingPrice = startingPrice;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ItemDTO getItemDto()         { return itemDto; }
    public double getBidStep()          { return bidStep; }
    public long getStartTime()    { return startTime; }
    public long getEndTime()      { return endTime; }
    public double getStartingPrice() { return startingPrice;}
}
