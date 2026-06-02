package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.item.Art;
import com.auction.server.model.item.Electronics;
import com.auction.server.model.item.Item;
import com.auction.server.model.item.Vehicle;
import com.auction.server.model.user.User;
import com.auction.shared.enums.ItemType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {
    private static volatile ItemRepository instance;

    private final UserRepository userRepository = UserRepository.getInstance();

    private ItemRepository() {}

    public static ItemRepository getInstance() {
        if (instance == null) {
            synchronized (ItemRepository.class) {
                if (instance == null) instance = new ItemRepository();
            }
        }
        return instance;
    }

    public void addItem(Item item) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            addItem(conn, item);
        }
    }

    public void addItem(Connection conn, Item item) throws SQLException {
        String sql = "INSERT INTO items (owner_id, name, description, image_url, start_price, item_type, " +
                "brand, warranty_months, artist, creation_year, vin, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getOwner().getId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            setNullableString(pstmt, 4, item.getImageUrl());
            pstmt.setDouble(5, item.getStartPrice());

            bindSubtypeColumns(pstmt, item);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert item affected no rows.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Insert item did not return generated id.");
                }
            }
        }
    }

    public void updateItem(Connection conn, Item item) throws SQLException {
        String sql = "UPDATE items SET name = ?, description = ?, image_url = ?, "
                + "brand = ?, warranty_months = ?, artist = ?, creation_year = ?, vin = ?, mileage = ? "
                + "WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            setNullableString(pstmt, 3, item.getImageUrl());

            if (item instanceof Electronics electronics) {
                pstmt.setString(4, electronics.getBrand());
                pstmt.setInt(5, electronics.getWarrantyMonths());
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setNull(7, Types.INTEGER);
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.INTEGER);
            } else if (item instanceof Art art) {
                pstmt.setNull(4, Types.VARCHAR);
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setString(6, art.getArtist());
                pstmt.setInt(7, art.getYearCreated());
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.INTEGER);
            } else if (item instanceof Vehicle vehicle) {
                pstmt.setString(4, vehicle.getBrand());
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setNull(7, Types.INTEGER);
                pstmt.setString(8, vehicle.getVin());
                pstmt.setInt(9, vehicle.getMileage());
            } else {
                throw new SQLException("Unsupported item class: " + item.getClass().getName());
            }

            pstmt.setInt(10, item.getId());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No item found for id=" + item.getId());
            }
        }
    }

    public Item getItemById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getItemById(conn, id);
        }
    }

    public Item getItemById(Connection conn, int id) throws Exception {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Item> getItemsByOwnerId(int ownerId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getItemsByOwnerId(conn, ownerId);
        }
    }

    public List<Item> getItemsByOwnerId(Connection conn, int ownerId) throws Exception {
        String sql = "SELECT * FROM items WHERE owner_id = ?";
        List<Item> items = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(conn, rs));
                }
            }
        }
        return items;
    }

    public List<Item> getAllItems() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAllItems(conn);
        }
    }

    public List<Item> getAllItems(Connection conn) throws Exception {
        String sql = "SELECT * FROM items";
        List<Item> items = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapResultSetToItem(conn, rs));
            }
        }
        return items;
    }

    public Item mapResultSetToItem(Connection conn, ResultSet rs) throws Exception {
        User owner = userRepository.getUserById(conn, rs.getInt("owner_id"));
        if (owner == null) {
            throw new SQLException("Item owner not found for owner_id=" + rs.getInt("owner_id"));
        }

        ItemType type = parseSupportedItemType(rs.getString("item_type"));

        return switch (type) {
            case ELECTRONICS -> new Electronics(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    owner,
                    rs.getDouble("start_price"),
                    rs.getString("image_url"),
                    rs.getString("brand"),
                    rs.getInt("warranty_months")
            );
            case ART -> new Art(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    owner,
                    rs.getDouble("start_price"),
                    rs.getString("image_url"),
                    rs.getString("artist"),
                    rs.getInt("creation_year")
            );
            case VEHICLE -> new Vehicle(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    owner,
                    rs.getDouble("start_price"),
                    rs.getString("image_url"),
                    rs.getString("brand"),
                    rs.getString("vin"),
                    rs.getInt("mileage")
            );
            default -> throw new SQLException("Unsupported item_type: " + type);
        };
    }

    private void bindSubtypeColumns(PreparedStatement pstmt, Item item) throws SQLException {
        if (item instanceof Electronics elec) {
            pstmt.setString(6, ItemType.ELECTRONICS.name());
            pstmt.setString(7, elec.getBrand());
            pstmt.setInt(8, elec.getWarrantyMonths());
            pstmt.setNull(9, Types.VARCHAR);
            pstmt.setNull(10, Types.INTEGER);
            pstmt.setNull(11, Types.VARCHAR);
            pstmt.setNull(12, Types.INTEGER);
            return;
        }

        if (item instanceof Art art) {
            pstmt.setString(6, ItemType.ART.name());
            pstmt.setNull(7, Types.VARCHAR);
            pstmt.setNull(8, Types.INTEGER);
            pstmt.setString(9, art.getArtist());
            pstmt.setInt(10, art.getYearCreated());
            pstmt.setNull(11, Types.VARCHAR);
            pstmt.setNull(12, Types.INTEGER);
            return;
        }

        if (item instanceof Vehicle vehicle) {
            pstmt.setString(6, ItemType.VEHICLE.name());
            pstmt.setString(7, vehicle.getBrand());
            pstmt.setNull(8, Types.INTEGER);
            pstmt.setNull(9, Types.VARCHAR);
            pstmt.setNull(10, Types.INTEGER);
            pstmt.setString(11, vehicle.getVin());
            pstmt.setInt(12, vehicle.getMileage());
            return;
        }

        throw new SQLException("Unsupported item class: " + item.getClass().getName());
    }

    private void setNullableString(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.VARCHAR);
        } else {
            pstmt.setString(index, value);
        }
    }

    private ItemType parseSupportedItemType(String rawType) throws SQLException {
        try {
            return ItemType.valueOf(rawType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SQLException("Unsupported item_type: " + rawType, e);
        }
    }
}
