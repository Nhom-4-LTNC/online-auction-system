package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.model.item.ItemType;
import com.auction.repository.ItemRepository;

import java.util.List;
import java.util.Map;

/*
 * ItemService — tầng Service cho Sản Phẩm
 *
 * Trách nhiệm:
 *   1. Tạo sản phẩm qua ItemFactory (phân loại: Electronics / Art / Vehicle)
 *   2. Lưu sản phẩm vào file qua ItemRepository
 *   3. Truy vấn danh sách sản phẩm
 *
 * Luồng dữ liệu: Controller → ItemService → ItemRepository → items.dat
 */
public class ItemService {

    // Singleton: cả ứng dụng chỉ dùng một instance
    private static volatile ItemService instance;

    private final ItemRepository itemRepository = ItemRepository.getInstance();

    private ItemService() {}

    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) instance = new ItemService();
            }
        }
        return instance;
    }


    // itemData phải chứa: "name", "price", "desc", "owner", "id"
    // Tùy loại còn cần thêm: "brand", "warranty" (Electronics) / "artist", "year" (Art) / "vin", "mileage" (Vehicle)
    public Item createItem(ItemType type, Map<String, Object> itemData) throws Exception {
        Item item = ItemFactory.createItem(type, itemData);
        if (!item.isValid())
            throw new Exception("Thông tin sản phẩm không hợp lệ!");
        itemRepository.addItem(item);
        return item;
    }

    public List<Item> getAllItems() {
        return itemRepository.getAllItems();
    }

    public List<Item> getItemsBySeller(int sellerId) {
        return itemRepository.getItemsBySeller(sellerId);
    }

    public Item getItemById(int itemId) throws Exception {
        Item item = itemRepository.getItemById(itemId);
        if (item == null)
            throw new Exception("Không tìm thấy sản phẩm với ID: " + itemId);
        return item;
    }
}
