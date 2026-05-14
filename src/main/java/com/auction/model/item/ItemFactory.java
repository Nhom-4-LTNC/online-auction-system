package com.auction.model.item;

import com.auction.model.user.User;
import com.auction.dto.ItemDTO;
import com.auction.dto.ElectronicsDTO;
import com.auction.dto.ArtDTO;
import com.auction.dto.VehicleDTO;

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
     * @return Đối tượng Item cụ thể (Electronics, Art, hoặc Vehicle)
     * @throws IllegalArgumentException nếu DTO không hợp lệ, bị null hoặc loại không được hỗ trợ
     * @throws Exception nếu logic của constructor các lớp con ném ra lỗi (ví dụ giá < 0)
     */
    public static Item createItem(ItemDTO dto, User owner, int id) throws Exception {

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
        switch (dto.getType()) {
            case ELECTRONICS:
                // Kiểm tra an toàn trước khi ép kiểu
                if (!(dto instanceof ElectronicsDTO)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Điện tử!");
                }
                ElectronicsDTO elecDto = (ElectronicsDTO) dto;

                // Trả về đối tượng Đồ điện tử
                return new Electronics(
                        id,
                        elecDto.getName(),
                        elecDto.getDescription(),
                        owner,
                        elecDto.getStartingPrice(),
                        elecDto.getBrand(),
                        elecDto.getWarrantyMonths()
                );

            case ART:
                if (!(dto instanceof ArtDTO)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Nghệ thuật!");
                }
                ArtDTO artDto = (ArtDTO) dto;

                // Trả về đối tượng Art
                return new Art(
                        id,
                        artDto.getName(),
                        artDto.getDescription(),
                        owner,
                        artDto.getStartingPrice(),
                        artDto.getArtist(),
                        artDto.getYearCreated()
                );

            case VEHICLE:
                if (!(dto instanceof VehicleDTO)) {
                    throw new IllegalArgumentException("Dữ liệu truyền vào không khớp với loại Phương tiện!");
                }
                VehicleDTO vehicleDto = (VehicleDTO) dto;

                // Trả về đối tượng Vehicle
                return new Vehicle(
                        id,
                        vehicleDto.getName(),
                        vehicleDto.getDescription(),
                        owner,
                        vehicleDto.getStartingPrice(),
                        vehicleDto.getBrand(),
                        vehicleDto.getVin(),
                        vehicleDto.getMileage()
                );

            default:
                throw new IllegalArgumentException("Hệ thống chưa hỗ trợ loại sản phẩm này: " + dto.getType());
        }
    }
}