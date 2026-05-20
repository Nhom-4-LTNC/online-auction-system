package com.auction.server.service;

import com.auction.shared.dto.ArtDTO;
import com.auction.shared.dto.ElectronicsDTO;
import com.auction.shared.dto.ItemDTO;
import com.auction.shared.dto.VehicleDTO;
import com.auction.shared.enums.ItemType;
import com.auction.server.model.item.*;
import com.auction.shared.util.ItemFactory;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Tầng Service cho nghiệp vụ quản lý sản phẩm (Item).
 *
 * <p>Trách nhiệm chính (SRP — chỉ điều phối luồng nghiệp vụ liên quan đến Item):
 * <ul>
 *   <li>Tạo sản phẩm qua {@link ItemFactory} và lưu xuống DB qua {@link ItemRepository}.</li>
 *   <li>Cung cấp các phương thức truy vấn sản phẩm theo ID, theo chủ sở hữu, hoặc toàn bộ.</li>
 * </ul>
 * Validation đầu vào được uỷ quyền cho {@link AuctionValidator}.</p>
 *
 * <p>Luồng dữ liệu: Controller → ItemService → ItemRepository → Database</p>
 */
public class ItemService {

    private static volatile ItemService instance;

    private final ItemRepository itemRepository = ItemRepository.getInstance();

    private ItemService() {}

    /**
     * Trả về instance duy nhất của {@code ItemService} (double-checked locking).
     *
     * @return instance singleton của {@code ItemService}
     */
    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) instance = new ItemService();
            }
        }
        return instance;
    }

    /**
     * Tạo một sản phẩm mới và lưu vào cơ sở dữ liệu.
     *
     * <p>Thứ tự thực hiện:
     * <ol>
     *   <li>Tạo {@link Item} qua {@link ItemFactory} với ID tạm {@code 0}.</li>
     *   <li>Validate sản phẩm vừa tạo qua {@link AuctionValidator#validateItem}.</li>
     *   <li>Lưu vào DB — {@link ItemRepository#addItem} gán ID thật vào object.</li>
     * </ol>
     * </p>
     *
     * @param seller  người sở hữu sản phẩm; không được {@code null}
     * @param itemDto DTO chứa dữ liệu sản phẩm (ElectronicsDTO, ArtDTO, VehicleDTO)
     * @return đối tượng {@link Item} vừa được tạo và lưu (đã có ID thật từ DB)
     * @throws IllegalArgumentException nếu dữ liệu sản phẩm không hợp lệ
     * @throws Exception                nếu xảy ra lỗi khi lưu vào DB
     */
    public Item createItem(User seller, ItemDTO itemDto) throws Exception {
        String imageUrl = saveImage(itemDto.getImageData(), itemDto.getImageFileName());

        Item item = ItemFactory.createItem(itemDto, seller, 0, imageUrl);

        AuctionValidator.validateItem(item);
        itemRepository.addItem(item);

        return item;
    }
    public String saveImage(byte[] imageData, String imageFileName) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }

        try {
            String extension = getSafeExtension(imageFileName);
            String newFileName =  UUID.randomUUID() + extension;

            Path uploadDir = Paths.get("uploads", "items");
            Files.createDirectories(uploadDir);

            Path targetPath = uploadDir.resolve(newFileName);
            Files.write(targetPath, imageData);

            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu ảnh", e);
        }
    }

    private String getSafeExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ".png";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return ".png";
        }

        String extension = fileName.substring(dotIndex).toLowerCase();

        return switch (extension) {
            case ".png", ".jpg", ".jpeg" -> extension;
            default -> ".png";
        };
    }
    /**
     * Tìm và trả về sản phẩm theo ID.
     *
     * @param id ID của sản phẩm cần tìm
     * @return đối tượng {@link Item} tương ứng
     * @throws Exception nếu không tìm thấy sản phẩm hoặc xảy ra lỗi DB
     */
    public Item getItemById(int id) throws Exception {
        Item item = itemRepository.getItemById(id);
        if (item == null) {
            throw new Exception("Không tìm thấy sản phẩm với ID: " + id);
        }
        return item;
    }

    /**
     * Trả về danh sách tất cả sản phẩm thuộc về một người bán.
     *
     * @param owner người sở hữu cần truy vấn; không được {@code null}
     * @return danh sách {@link Item}; trả về danh sách rỗng nếu người bán chưa có sản phẩm nào
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi DB
     */
    public List<Item> getItemsByOwner(User owner) throws Exception {
        return itemRepository.getItemsByOwnerId(owner.getId());
    }

    /**
     * Trả về danh sách tất cả sản phẩm trong hệ thống.
     *
     * @return danh sách {@link Item}; trả về danh sách rỗng nếu chưa có sản phẩm nào
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi DB
     */
    public List<Item> getAllItems() throws Exception {
        return itemRepository.getAllItems();
    }

    public ItemDTO mapToItemDTO(Item item) {
        ItemType type = ItemType.valueOf(item.getCategory());
        byte[] imageData = readImage(item.getImageUrl());
        String imageFileName = getFileName(item.getImageUrl());

        return switch (type) {
            case ELECTRONICS -> {
                if (!(item instanceof Electronics electronics)) {
                    throw new ClassCastException();
                }
                yield new ElectronicsDTO(
                        electronics.getName(),
                        electronics.getDescription(),
                        electronics.getStartPrice(),
                        imageData,
                        imageFileName,
                        electronics.getBrand(),
                        electronics.getWarrantyMonths()
                );
            }
            case ART -> {
                if (!(item instanceof Art art)) {
                    throw new ClassCastException();
                }

                yield new ArtDTO(
                        art.getName(),
                        art.getDescription(),
                        art.getStartPrice(),
                        imageData,
                        imageFileName,
                        art.getArtist(),
                        art.getYearCreated()
                );
            }
            case VEHICLE -> {

                if (!(item instanceof Vehicle vehicle)) {
                    throw new ClassCastException();
                }

                yield new VehicleDTO(
                        vehicle.getName(),
                        vehicle.getDescription(),
                        vehicle.getStartPrice(),
                        imageData,
                        imageFileName,
                        vehicle.getBrand(),
                        vehicle.getVin(),
                        vehicle.getMileage()
                );
            }
        };
    }
    private byte[] readImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            return Files.readAllBytes(Paths.get(imageUrl));
        } catch (IOException e) {
            return null;
        }
    }

    private String getFileName(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return Paths.get(imageUrl).getFileName().toString();
    }

}
