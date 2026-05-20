package com.auction.repository;

import com.auction.config.DatabaseConnection;
import com.auction.model.item.*;
import com.auction.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository xử lý các thao tác CRUD đối với bảng {@code items} trong cơ sở dữ liệu.
 *
 * <p>Hỗ trợ ba loại vật phẩm đa hình: {@link Electronics}, {@link Art} và {@link Vehicle}.
 * Mỗi loại có các cột riêng trong bảng; các cột không dùng sẽ được lưu là {@code NULL}.</p>
 *
 * <p>Lớp này được triển khai theo mẫu Singleton thread-safe (double-checked locking).</p>
 */
public class ItemRepository {
    private static volatile ItemRepository instance;

    private ItemRepository() {}

    /**
     * Trả về instance duy nhất của {@code ItemRepository}.
     *
     * <p>Sử dụng double-checked locking để đảm bảo an toàn trong môi trường đa luồng.</p>
     *
     * @return instance singleton của {@code ItemRepository}
     */
    public static ItemRepository getInstance() {
        if (instance == null) {
            synchronized (ItemRepository.class) {
                if (instance == null) instance = new ItemRepository();
            }
        }
        return instance;
    }

    /**
     * Thêm một vật phẩm mới vào bảng {@code items}.
     *
     * <p>Phương thức tự động phát hiện kiểu con của {@code item} ({@link Electronics},
     * {@link Art} hoặc {@link Vehicle}) để điền đúng các cột đặc thù. Sau khi INSERT
     * thành công, ID được sinh tự động từ database sẽ được gán lại vào đối tượng
     * qua {@link Item#setId(int)}.</p>
     *
     * @param item đối tượng vật phẩm cần lưu; không được {@code null}
     * @throws Exception nếu INSERT không thành công, không lấy được ID sinh tự động,
     *                   hoặc xảy ra lỗi kết nối / lỗi SQL
     */
    public void addItem(Item item) throws Exception {
        String sql = "INSERT INTO items (owner_id, name, description, image_url, start_price, item_type, " +
                "brand, warranty_months, artist, creation_year, vin, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.getOwner().getId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());

            if (item.getImageUrl() != null) {
                pstmt.setString(4, item.getImageUrl());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            pstmt.setDouble(5, item.getStartPrice());

            if (item instanceof Electronics) {
                Electronics elec = (Electronics) item;
                pstmt.setString(6, ItemType.ELECTRONICS.name());
                pstmt.setString(7, elec.getBrand());
                pstmt.setInt(8, elec.getWarrantyMonths());
                pstmt.setNull(9, Types.VARCHAR);
                pstmt.setNull(10, Types.INTEGER);
                pstmt.setNull(11, Types.VARCHAR);
                pstmt.setNull(12, Types.INTEGER);

            } else if (item instanceof Art) {
                Art art = (Art) item;
                pstmt.setString(6, ItemType.ART.name());
                pstmt.setNull(7, Types.VARCHAR);
                pstmt.setNull(8, Types.INTEGER);
                pstmt.setString(9, art.getArtist());
                pstmt.setInt(10, art.getYearCreated());
                pstmt.setNull(11, Types.VARCHAR);
                pstmt.setNull(12, Types.INTEGER);

            } else if (item instanceof Vehicle) {
                Vehicle vehicle = (Vehicle) item;
                pstmt.setString(6, ItemType.VEHICLE.name());
                pstmt.setString(7, vehicle.getBrand());
                pstmt.setNull(8, Types.INTEGER);
                pstmt.setNull(9, Types.VARCHAR);
                pstmt.setNull(10, Types.INTEGER);
                pstmt.setString(11, vehicle.getVin());
                pstmt.setInt(12, vehicle.getMileage());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Lệnh INSERT không thêm được dòng nào.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Không lấy được ID tự động sinh ra.");
                }
            }

        } catch (SQLException e) {
            System.err.println("[ItemRepository - addItem] Lỗi cơ sở dữ liệu: " + e.getMessage());
            throw new Exception("Lỗi hệ thống khi thêm sản phẩm: " + e.getMessage(), e);
        }
    }

    /**
     * Truy vấn và trả về vật phẩm theo ID.
     *
     * @param id ID của vật phẩm cần tìm
     * @return đối tượng {@link Item} tương ứng, hoặc {@code null} nếu không tồn tại
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL trong quá trình truy vấn
     */
    public Item getItemById(int id) throws Exception {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemRepository - getItemById] Lỗi cơ sở dữ liệu: " + e.getMessage());
            throw new Exception("Không thể lấy thông tin sản phẩm: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Truy vấn và trả về tất cả vật phẩm thuộc về một chủ sở hữu.
     *
     * @param ownerId ID của người sở hữu
     * @return danh sách {@link Item}; trả về danh sách rỗng nếu không có sản phẩm nào
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL
     */
    public List<Item> getItemsByOwnerId(int ownerId) throws Exception {
        String sql = "SELECT * FROM items WHERE owner_id = ?";
        List<Item> items = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemRepository - getItemsByOwnerId] Lỗi cơ sở dữ liệu: " + e.getMessage());
            throw new Exception("Không thể lấy danh sách sản phẩm theo chủ sở hữu: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Truy vấn và trả về toàn bộ vật phẩm trong cơ sở dữ liệu.
     *
     * @return danh sách tất cả {@link Item}; trả về danh sách rỗng nếu bảng không có dữ liệu
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL
     */
    public List<Item> getAllItems() throws Exception {
        String sql = "SELECT * FROM items";
        List<Item> items = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ItemRepository - getAllItems] Lỗi cơ sở dữ liệu: " + e.getMessage());
            throw new Exception("Không thể lấy danh sách sản phẩm: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Ánh xạ một hàng kết quả từ {@link ResultSet} sang đối tượng {@link Item} cụ thể.
     *
     * <p>Đọc cột {@code item_type} để xác định kiểu con ({@link Electronics}, {@link Art}
     * hoặc {@link Vehicle}), tự động load chủ sở hữu qua {@link UserRepository}.</p>
     *
     * @param rs {@link ResultSet} đang trỏ đến hàng cần đọc
     * @return đối tượng {@link Item} tương ứng với hàng hiện tại
     * @throws Exception nếu xảy ra lỗi đọc dữ liệu hoặc load người dùng
     */
    private Item mapResultSetToItem(ResultSet rs) throws Exception {
        User owner = UserRepository.getInstance().getUserById(rs.getInt("owner_id"));
        ItemType type = ItemType.valueOf(rs.getString("item_type"));

        Item item;
        switch (type) {
            case ELECTRONICS:
                item = new Electronics(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        owner,
                        rs.getDouble("start_price"),
                        rs.getString("image_url"),
                        rs.getString("brand"),
                        rs.getInt("warranty_months")
                );
                break;
            case ART:
                item = new Art(
                        rs.getInt("id"), rs.getString("name"),
                        rs.getString("description"),
                        owner,
                        rs.getDouble("start_price"),
                        rs.getString("image_url"),
                        rs.getString("artist"),
                        rs.getInt("creation_year")
                );
                break;
            case VEHICLE:
                item = new Vehicle(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        owner,
                        rs.getDouble("start_price"),
                        rs.getString("image_url"),
                        rs.getString("brand"),
                        rs.getString("vin"), rs.getInt("mileage")
                );
                break;
            default:
                throw new SQLException("Loại sản phẩm không được hỗ trợ: " + type);
        }
        return item;
    }
}
