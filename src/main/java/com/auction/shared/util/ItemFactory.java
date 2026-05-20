package com.auction.shared.util;

import com.auction.server.model.item.Art;
import com.auction.server.model.item.Electronics;
import com.auction.server.model.item.Item;
import com.auction.server.model.item.Vehicle;
import com.auction.server.model.user.User;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.VehicleDTO;

/**
 * Factory class chịu trách nhiệm khởi tạo các đối tượng Item cụ thể.
 * Áp dụng Factory Method Pattern.
 */
public class ItemFactory {

    /**
     * Tạo một sản phẩm mới dựa trên DTO từ Client gửi lên.
     *
     * @param dto   Dữ liệu sản phẩm (chứa thông tin chung và riêng theo từng loại)
     * @param owner Người bán (chủ sở hữu)
     * @param id    ID tự tăng của sản phẩm (do hệ thống cấp)
     * @param imageUrl Đường dẫn đến hình ảnh sản phẩm
     * @return Đối tượng Item cụ thể (Electronics, Art, hoặc Vehicle)
     * @throws IllegalArgumentException nếu DTO không hợp lệ, bị null hoặc loại không được hỗ trợ
     * @throws Exception nếu logic của constructor các lớp con ném ra lỗi (ví dụ giá < 0)
     */
    public static Item createItem(ItemDTO dto, User owner, int id, String imageUrl) throws Exception {

        // 1. Kiểm tra dữ liệu đầu vào (Fail-fast)
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm (DTO) không được để trống!");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Người bán (owner) không được để trống!");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Loại sản phẩm (ItemType) chưa được xác định!");
        }

        // 2. Dựa vào ItemType để khởi tạo class tương ứng
        return switch (dto.getType()) {
            case ELECTRONICS:
                // Kiểm tra an toàn trước khi ép kiểu
                if (!(dto instanceof ElectronicsDTO electDto)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Điện tử!");
                }

                // Trả về đối tượng Đồ điện tử
                yield new Electronics(
                        id,
                        electDto.getName(),
                        electDto.getDescription(),
                        owner,
                        electDto.getStartingPrice(),
                        imageUrl,
                        electDto.getBrand(),
                        electDto.getWarrantyMonths()
                );

            case ART:
                if (!(dto instanceof ArtDTO artDto)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Nghệ thuật!");
                }

                // Trả về đối tượng Art
                yield new Art(
                        id,
                        artDto.getName(),
                        artDto.getDescription(),
                        owner,
                        artDto.getStartingPrice(),
                        imageUrl,
                        artDto.getArtist(),
                        artDto.getYearCreated()
                );

            case VEHICLE:
                if (!(dto instanceof VehicleDTO vehicleDto)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Phương tiện!");
                }

                // Trả về đối tượng Vehicle
                yield new Vehicle(
                        id,
                        vehicleDto.getName(),
                        vehicleDto.getDescription(),
                        owner,
                        vehicleDto.getStartingPrice(),
                        imageUrl,
                        vehicleDto.getBrand(),
                        vehicleDto.getVin(),
                        vehicleDto.getMileage()
                );

            default:
                throw new IllegalArgumentException("Hệ thống chưa hỗ trợ loại sản phẩm này: " + dto.getType());
        };
    }
    public static Item createItem(ItemDTO dto, User owner, int id) throws Exception {
        return createItem(dto, owner, id, null);
    }
}