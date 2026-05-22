package com.auction.server.service;

import com.auction.server.model.item.Item;
import com.auction.shared.exception.ValidationException;

/**
 * Chịu trách nhiệm duy nhất: kiểm tra tính hợp lệ của các tham số
 * trước khi tạo phiên đấu giá.
 *
 * <p>Tách biệt khỏi {@link AuctionService} theo nguyên tắc SRP — service chỉ
 * điều phối luồng nghiệp vụ, lớp này chỉ thực hiện validation.</p>
 */
public class AuctionValidator {

    private AuctionValidator() {}

    /**
     * Kiểm tra tính hợp lệ của thời gian và bước giá cho một phiên đấu giá mới.
     *
     * <p>Quy tắc:
     * <ul>
     *   <li>{@code endTimeMillis} phải lớn hơn {@code startTimeMillis}.</li>
     *   <li>{@code endTimeMillis} phải lớn hơn thời điểm hiện tại.</li>
     *   <li>{@code bidStep} phải lớn hơn {@code 0}.</li>
     * </ul>
     * </p>
     *
     * @param startTimeMillis thời điểm bắt đầu (ms epoch)
     * @param endTimeMillis   thời điểm kết thúc (ms epoch)
     * @param bidStep         bước giá tối thiểu
     * @throws ValidationException nếu bất kỳ điều kiện nào bị vi phạm
     */
    public static void validateAuctionParams(long startTimeMillis, long endTimeMillis, double bidStep) throws ValidationException{
        long now = System.currentTimeMillis();

        if (endTimeMillis <= startTimeMillis) {
            throw new ValidationException("Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
        }
        if (endTimeMillis <= now) {
            throw new ValidationException("Thời gian kết thúc phải lớn hơn thời gian hiện tại!");
        }
        if (bidStep <= 0) {
            throw new ValidationException("Bước giá phải lớn hơn 0!");
        }
    }

    /**
     * Kiểm tra tính hợp lệ của đối tượng {@link Item} vừa được tạo.
     *
     * @param item đối tượng sản phẩm cần kiểm tra
     * @throws ValidationException nếu {@code item} là {@code null} hoặc không hợp lệ
     */
    public static void validateItem(Item item) throws ValidationException{
        if (item == null || !item.isValid()) {
            throw new ValidationException("Thông tin sản phẩm không hợp lệ hoặc thiếu dữ liệu bắt buộc!");
        }
    }
}
