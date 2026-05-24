package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {
    private static volatile AuctionRepository instance;

    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private AuctionRepository() {}

    public static AuctionRepository getInstance() {
        if (instance == null) {
            synchronized (AuctionRepository.class) {
                if (instance == null) instance = new AuctionRepository();
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            addAuction(conn, auction);
        }
    }

    public void addAuction(Connection conn, Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (item_id, starting_price, bid_step, current_price, " +
                "last_bidder_id, start_time, end_time, status, winner_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, auction.getItem().getId());
            pstmt.setDouble(2, auction.getStartPrice());
            pstmt.setDouble(3, auction.getBidStep());
            pstmt.setDouble(4, auction.getCurrentPrice());
            setNullableUserId(pstmt, 5, auction.getLastBidder());
            pstmt.setTimestamp(6, new Timestamp(auction.getStartTime()));
            pstmt.setTimestamp(7, new Timestamp(auction.getEndTime()));
            pstmt.setString(8, auction.getStatus().name());
            setNullableUserId(pstmt, 9, auction.getWinner());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert auction affected no rows.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auction.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Insert auction did not return generated id.");
                }
            }
        }
    }

    public void updateAuction(Auction auction) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateAuction(conn, auction);
        }
    }

    public void updateAuction(Connection conn, Auction auction) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, last_bidder_id = ?, winner_id = ?, status = ? " +
                "WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, auction.getCurrentPrice());
            setNullableUserId(pstmt, 2, auction.getLastBidder());
            setNullableUserId(pstmt, 3, auction.getWinner());
            pstmt.setString(4, auction.getStatus().name());
            pstmt.setInt(5, auction.getId());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No auction found for id=" + auction.getId());
            }
        }
    }

    public Auction getAuctionById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAuctionById(conn, id);
        }
    }

    public Auction getAuctionById(Connection conn, int id) throws Exception {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(conn, rs);
                }
            }
        }
        return null;
    }

    public Auction findByIdForUpdate(Connection conn, int id) throws Exception {
        String sql = "SELECT * FROM auctions WHERE id = ? FOR UPDATE";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Auction> getAllAuctions() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAllAuctions(conn);
        }
    }

    public List<Auction> getAuctionsByType(Connection conn, String type) throws Exception {
         String sql = "SELECT * FROM auctions WHERE item_type = ?";
         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, type);
             return queryAuctions(conn, pstmt.toString());
         }
    }

    public List<Auction> getAllAuctions(Connection conn) throws Exception {
        String sql = "SELECT * FROM auctions";
        return queryAuctions(conn, sql);
    }
    public List<AuctionSummaryDTO> getAuctionSummariesByType(ItemType itemType) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAuctionSummariesByType(conn, itemType);
        }
    }

    public List<AuctionSummaryDTO> getAuctionSummariesByType(Connection conn, ItemType itemType) throws SQLException {
        String sql = """
            SELECT
                a.id AS auction_id,
                i.id AS item_id,
                i.name AS item_name,
                i.item_type AS item_type,
                a.current_price AS current_price,
                a.end_time AS end_time,
                a.status AS status
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE i.item_type = ?
            ORDER BY a.end_time ASC
            """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new AuctionSummaryDTO(
                            rs.getInt("auction_id"),
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            ItemType.valueOf(rs.getString("item_type")),
                            rs.getDouble("current_price"),
                            rs.getTimestamp("end_time").getTime(),
                            AuctionStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
        }

        return result;
    }
    public List<Auction> getActiveAuctions() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getActiveAuctions(conn);
        }
    }

    public List<Auction> getActiveAuctions(Connection conn) throws Exception {
        String sql = "SELECT * FROM auctions WHERE status IN (?, ?)";
        List<Auction> auctions = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, AuctionStatus.OPEN.name());
            pstmt.setString(2, AuctionStatus.RUNNING.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(conn, rs));
                }
            }
        }
        return auctions;
    }

    public Auction mapResultSetToAuction(Connection conn, ResultSet rs) throws Exception {
        Item item = itemRepository.getItemById(conn, rs.getInt("item_id"));
        if (item == null) {
            throw new SQLException("Auction item not found for item_id=" + rs.getInt("item_id"));
        }

        Auction auction = new Auction(
                rs.getInt("id"),
                item,
                rs.getDouble("bid_step"),
                rs.getTimestamp("start_time").getTime(),
                rs.getTimestamp("end_time").getTime()
        );
        auction.setStartPrice(rs.getDouble("starting_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));

        int lastBidderId = rs.getInt("last_bidder_id");
        if (!rs.wasNull()) {
            auction.setLastBidder(userRepository.getUserById(conn, lastBidderId));
        }

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            auction.setWinner(userRepository.getUserById(conn, winnerId));
        }

        String status = rs.getString("status");
        if (status != null) {
            auction.setStatus(AuctionStatus.valueOf(status));
        }

        return auction;
    }

    private List<Auction> queryAuctions(Connection conn, String sql) throws Exception {
        List<Auction> list = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToAuction(conn, rs));
            }
        }
        return list;
    }

    private void setNullableUserId(PreparedStatement pstmt, int index, User user) throws SQLException {
        if (user == null) {
            pstmt.setNull(index, Types.INTEGER);
        } else {
            pstmt.setInt(index, user.getId());
        }
    }
}
