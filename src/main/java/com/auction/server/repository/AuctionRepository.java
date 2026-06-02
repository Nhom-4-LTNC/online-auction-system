package com.auction.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;

public class AuctionRepository {

    private static volatile AuctionRepository instance;

    private final ItemRepository itemRepository = ItemRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private AuctionRepository() {
    }

    public static AuctionRepository getInstance() {
        if (instance == null) {
            synchronized (AuctionRepository.class) {
                if (instance == null) {
                    instance = new AuctionRepository();
                }
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
        String sql = "INSERT INTO auctions (item_id, starting_price, bid_step, current_price, "
                + "last_bidder_id, start_time, end_time, status, winner_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        String sql = "UPDATE auctions SET current_price = ?, last_bidder_id = ?, winner_id = ?, status = ? "
                + "WHERE id = ?";

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

    public void updateAuctionSchedule(Connection conn, Auction auction) throws SQLException {
        String sql = "UPDATE auctions SET start_time = ?, end_time = ?, status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(auction.getStartTime()));
            pstmt.setTimestamp(2, new Timestamp(auction.getEndTime()));
            pstmt.setString(3, auction.getStatus().name());
            pstmt.setInt(4, auction.getId());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No auction found for id=" + auction.getId());
            }
        }
    }

    public void updateEndTime(Connection conn, int auctionId, long endTimeMillis) throws SQLException {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(endTimeMillis));
            pstmt.setInt(2, auctionId);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No auction found for id=" + auctionId);
            }
        }
    }

    public int finalizeExpiredAuctionsForRead(Connection conn, long nowMillis) throws SQLException {
        String sql = """
            UPDATE auctions
            SET status = ?, winner_id = COALESCE(winner_id, last_bidder_id)
            WHERE status IN (?, ?)
              AND end_time < ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AuctionStatus.FINISHED.name());
            stmt.setString(2, AuctionStatus.OPEN.name());
            stmt.setString(3, AuctionStatus.RUNNING.name());
            stmt.setTimestamp(4, new Timestamp(nowMillis));
            return stmt.executeUpdate();
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

    public List<Auction> findAuctionsDueForStatusTransition(Connection conn, long nowMillis) throws Exception {
        String sql = """
            SELECT *
            FROM auctions
            WHERE (status = ? AND start_time <= ?)
               OR (status = ? AND end_time <= ?)
            FOR UPDATE
            """;

        List<Auction> auctions = new ArrayList<>();
        Timestamp now = new Timestamp(nowMillis);

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, AuctionStatus.OPEN.name());
            pstmt.setTimestamp(2, now);
            pstmt.setString(3, AuctionStatus.RUNNING.name());
            pstmt.setTimestamp(4, now);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(conn, rs));
                }
            }
        }
        return auctions;
    }

    public List<Auction> getAllAuctions() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAllAuctions(conn);
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
                a.start_time AS start_time,
                a.end_time AS end_time,
                a.status AS status,
                a.winner_id AS winner_id,
                winner.username AS winner_username
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            LEFT JOIN users winner ON a.winner_id = winner.id
            WHERE i.item_type = ?
            ORDER BY a.end_time ASC
            """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemType.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuctionSummaryDTO(rs));
                }
            }
        }

        return result;
    }

    public List<AuctionSummaryDTO> findSummariesBySellerId(Connection conn, int sellerId) throws SQLException {
        String sql = """
            SELECT
                a.id AS auction_id,
                i.id AS item_id,
                i.name AS item_name,
                i.item_type AS item_type,
                a.current_price AS current_price,
                a.start_time AS start_time,
                a.end_time AS end_time,
                a.status AS status,
                a.winner_id AS winner_id,
                winner.username AS winner_username
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            LEFT JOIN users winner ON a.winner_id = winner.id
            WHERE i.owner_id = ?
            ORDER BY a.start_time DESC
            """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuctionSummaryDTO(rs));
                }
            }
        }

        return result;
    }

    public List<AuctionSummaryDTO> findSummariesParticipatedByBidderId(Connection conn, int bidderId)
            throws SQLException {
        String sql = """
            SELECT
                a.id AS auction_id,
                i.id AS item_id,
                i.name AS item_name,
                i.item_type AS item_type,
                a.current_price AS current_price,
                a.start_time AS start_time,
                a.end_time AS end_time,
                a.status AS status,
                a.winner_id AS winner_id,
                winner.username AS winner_username
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            LEFT JOIN users winner ON a.winner_id = winner.id
            WHERE EXISTS (
                SELECT 1
                FROM bids b
                WHERE b.auction_id = a.id
                  AND b.bidder_id = ?
            )
            ORDER BY a.end_time DESC
            """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuctionSummaryDTO(rs));
                }
            }
        }

        return result;
    }

    public List<AuctionSummaryDTO> findSummariesWonByUserId(Connection conn, int userId)
            throws SQLException {
        String sql = """
            SELECT
                a.id AS auction_id,
                i.id AS item_id,
                i.name AS item_name,
                i.item_type AS item_type,
                a.current_price AS current_price,
                a.start_time AS start_time,
                a.end_time AS end_time,
                a.status AS status,
                a.winner_id AS winner_id,
                winner.username AS winner_username
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            LEFT JOIN users winner ON a.winner_id = winner.id
            WHERE a.winner_id = ?
              AND a.status IN (?, ?)
            ORDER BY a.end_time DESC
            """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, AuctionStatus.FINISHED.name());
            stmt.setString(3, AuctionStatus.PAID.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuctionSummaryDTO(rs));
                }
            }
        }

        return result;
    }

    public List<AuctionSummaryDTO> getAllAuctionSummaries() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAllAuctionSummaries(conn);
        }
    }

    public List<AuctionSummaryDTO> getAllAuctionSummaries(Connection conn) throws SQLException {
        // Câu lệnh SQL JOIN tối ưu: Lấy toàn bộ dữ liệu trong 1 lần duy nhất
        String sql = """
        SELECT
            a.id AS auction_id,
            i.id AS item_id,
            i.name AS item_name,
            i.item_type AS item_type,
            a.current_price AS current_price,
            a.start_time AS start_time,
            a.end_time AS end_time,
            a.status AS status,
            a.winner_id AS winner_id,
            winner.username AS winner_username
        FROM auctions a
        JOIN items i ON a.item_id = i.id
        LEFT JOIN users winner ON a.winner_id = winner.id
        ORDER BY a.end_time ASC
        """;

        List<AuctionSummaryDTO> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                // Map thẳng sang DTO theo đúng Constructor 7 tham số của bạn
                result.add(mapResultSetToAuctionSummaryDTO(rs));
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
    public double getUnpaidWinningAmount(Connection conn, int userId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(current_price), 0) AS unpaid_amount
                FROM auctions
                WHERE winner_id = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, AuctionStatus.FINISHED.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("unpaid_amount");
                }
            }
        }

        return 0;
    }

    public double getActiveLeadingAmountExcludingAuction(
            Connection conn,
            int userId,
            int excludedAuctionId
    ) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(current_price), 0) AS active_leading_amount
                FROM auctions
                WHERE last_bidder_id = ?
                  AND id <> ?
                  AND status IN (?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, excludedAuctionId);
            stmt.setString(3, AuctionStatus.OPEN.name());
            stmt.setString(4, AuctionStatus.RUNNING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("active_leading_amount");
                }
            }
        }

        return 0;
    }
    private Item mapJoinedRowToItem(Connection conn, ResultSet rs) throws Exception {
        // item columns
        ItemType type = ItemType.valueOf(rs.getString("item_type"));
        int ownerId = rs.getInt("owner_id");
        User owner = userRepository.getUserById(conn, ownerId);
        if (owner == null) {
            throw new SQLException("Item owner not found for owner_id=" + ownerId);
        }

        return switch (type) {
            case ELECTRONICS -> new com.auction.server.model.item.Electronics(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getString("item_description"),
                    owner,
                    rs.getDouble("start_price"),
                    rs.getString("image_url"),
                    rs.getString("brand"),
                    rs.getInt("warranty_months")
            );
            case ART -> new com.auction.server.model.item.Art(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getString("item_description"),
                    owner,
                    rs.getDouble("start_price"),
                    rs.getString("image_url"),
                    rs.getString("artist"),
                    rs.getInt("creation_year")
            );
            case VEHICLE -> new com.auction.server.model.item.Vehicle(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getString("item_description"),
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


    private Auction mapJoinedRowToAuction(Connection conn, ResultSet rs) throws Exception {
        Item item = mapJoinedRowToItem(conn, rs);

        Auction auction = new Auction(
                rs.getInt("auction_id"),
                item,
                rs.getDouble("bid_step"),
                rs.getTimestamp("start_time").getTime(),
                rs.getTimestamp("end_time").getTime()
        );
        auction.setStartPrice(rs.getDouble("starting_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));

        Integer lastBidderId = (Integer) (rs.getObject("last_bidder_id"));
        if (lastBidderId != null) {
            auction.setLastBidder(userRepository.getUserById(conn, lastBidderId));
        }

        Integer winnerId = (Integer) (rs.getObject("winner_id"));
        if (winnerId != null) {
            auction.setWinner(userRepository.getUserById(conn, winnerId));
        }

        String status = rs.getString("status");
        if (status != null) {
            auction.setStatus(AuctionStatus.valueOf(status));
        }

        return auction;
    }

    // Fast path to avoid N+1: fetch auctions with their related items and users in one query.
    public List<Auction> getAllAuctionsWithDetails(Connection conn) throws Exception {
        String sql = """
            SELECT
                a.id AS auction_id,
                a.item_id AS item_id,
                a.starting_price,
                a.current_price,
                a.bid_step,
                a.start_time,
                a.end_time,
                a.status,
                a.last_bidder_id,
                a.winner_id,

                i.name AS item_name,
                i.description AS item_description,
                i.image_url,
                i.item_type AS item_type,
                i.start_price,

                i.owner_id AS owner_id,

                -- subtype columns (only relevant ones will be non-null)
                COALESCE(i.brand, '') AS brand,
                i.warranty_months,
                i.artist,
                i.creation_year,
                i.vin,
                i.mileage
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            ORDER BY a.end_time ASC
            """;

        List<Auction> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapJoinedRowToAuction(conn, rs));
            }
        }
        return result;
    }

    public Auction mapResultSetToAuction(Connection conn, ResultSet rs) throws Exception {
        // Legacy slow mapping (kept for compatibility). Avoid using it in bulk flows.
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

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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

    private AuctionSummaryDTO mapResultSetToAuctionSummaryDTO(ResultSet rs) throws SQLException {
        return new AuctionSummaryDTO(
                rs.getInt("auction_id"),
                rs.getInt("item_id"),
                rs.getString("item_name"),
                ItemType.valueOf(rs.getString("item_type")),
                rs.getDouble("current_price"),
                rs.getTimestamp("start_time").getTime(),
                rs.getTimestamp("end_time").getTime(),
                AuctionStatus.valueOf(rs.getString("status")),
                getNullableInt(rs, "winner_id"),
                rs.getString("winner_username")
        );
    }

    private Integer getNullableInt(ResultSet rs, String columnLabel) throws SQLException {
        int value = rs.getInt(columnLabel);
        return rs.wasNull() ? null : value;
    }
}
