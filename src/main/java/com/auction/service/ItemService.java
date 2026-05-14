package com.auction.service;

import com.auction.dto.ItemDTO;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.model.user.User;
import com.auction.repository.ItemRepository;

import java.util.List;

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
        Item item = ItemFactory.createItem(itemDto, seller, 0);
        AuctionValidator.validateItem(item);
        itemRepository.addItem(item);
        return item;
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
}
