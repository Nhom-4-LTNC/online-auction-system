package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.Bid;
import com.auction.shared.dto.BidDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {
    private static volatile BidRepository instance;

    private BidRepository() {}

    public static BidRepository getInstance() {
        if (instance == null) {
            synchronized (BidRepository.class) {
                if (instance == null) {
                    instance = new BidRepository();
                }
            }
        }
        return instance;
    }

    public Bid save(Bid bid) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, bid);
        }
    }

    public Bid save(Connection conn, Bid bid) throws SQLException {
        String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, bid.getAuctionId());
            stmt.setInt(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4, Timestamp.from(Instant.ofEpochMilli(bid.getTimestamp())));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert bid affected no rows.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Insert bid did not return generated id.");
                }
            }
        }
        return bid;
    }

    public Bid findById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public Bid findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM bids WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }
        }
        return null;
    }

    public List<Bid> findByAuctionId(int auctionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByAuctionId(conn, auctionId);
        }
    }

    public List<Bid> findByAuctionId(Connection conn, int auctionId) throws SQLException {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time DESC";
        List<Bid> bids = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        }
        return bids;
    }

    public List<BidDTO> getBidDTOsByAuctionId(int auctionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getBidDTOsByAuctionId(conn, auctionId);
        }
    }

    public List<BidDTO> getBidDTOsByAuctionId(Connection conn, int auctionId) throws SQLException {
        String sql = """
                SELECT
                    b.id,
                    b.auction_id,
                    b.bidder_id,
                    u.username AS bidder_username,
                    b.bid_amount,
                    b.bid_time
                FROM bids b
                JOIN users u ON b.bidder_id = u.id
                WHERE b.auction_id = ?
                ORDER BY b.bid_time DESC
                """;
        List<BidDTO> bids = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBidDTO(rs));
                }
            }
        }
        return bids;
    }

    public List<BidDTO> getRecentBidDTOsByAuctionId(int auctionId, int limit) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getRecentBidDTOsByAuctionId(conn, auctionId, limit);
        }
    }

    public List<BidDTO> getRecentBidDTOsByAuctionId(Connection conn, int auctionId, int limit) throws SQLException {
        String sql = """
                SELECT
                    b.id,
                    b.auction_id,
                    b.bidder_id,
                    u.username AS bidder_username,
                    b.bid_amount,
                    b.bid_time
                FROM bids b
                JOIN users u ON b.bidder_id = u.id
                WHERE b.auction_id = ?
                ORDER BY b.bid_time DESC
                LIMIT ?
                """;
        List<BidDTO> bids = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBidDTO(rs));
                }
            }
        }
        return bids;
    }

    public Bid findHighestBidByAuctionId(int auctionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findHighestBidByAuctionId(conn, auctionId);
        }
    }

    public Bid findHighestBidByAuctionId(Connection conn, int auctionId) throws SQLException {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }
        }
        return null;
    }

    public List<Bid> findByBidderId(int bidderId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByBidderId(conn, bidderId);
        }
    }

    public List<Bid> findByBidderId(Connection conn, int bidderId) throws SQLException {
        String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY bid_time DESC";
        List<Bid> bids = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        }
        return bids;
    }

    public List<BidDTO> getBidDTOsByBidderId(int bidderId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getBidDTOsByBidderId(conn, bidderId);
        }
    }

    public List<BidDTO> getBidDTOsByBidderId(Connection conn, int bidderId) throws SQLException {
        String sql = """
                SELECT
                    b.id,
                    b.auction_id,
                    b.bidder_id,
                    u.username AS bidder_username,
                    b.bid_amount,
                    b.bid_time
                FROM bids b
                JOIN users u ON b.bidder_id = u.id
                WHERE b.bidder_id = ?
                ORDER BY b.bid_time DESC
                """;
        List<BidDTO> bids = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBidDTO(rs));
                }
            }
        }
        return bids;
    }

    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        Timestamp bidTime = rs.getTimestamp("bid_time");
        long timestamp = bidTime != null ? bidTime.toInstant().toEpochMilli() : 0L;
        return new Bid(
                rs.getInt("id"),
                rs.getInt("auction_id"),
                rs.getInt("bidder_id"),
                rs.getDouble("bid_amount"),
                timestamp
        );
    }

    private BidDTO mapResultSetToBidDTO(ResultSet rs) throws SQLException {
        Timestamp bidTime = rs.getTimestamp("bid_time");
        long timestamp = bidTime != null ? bidTime.toInstant().toEpochMilli() : 0L;
        return new BidDTO(
                rs.getInt("id"),
                rs.getInt("auction_id"),
                rs.getInt("bidder_id"),
                rs.getString("bidder_username"),
                rs.getDouble("bid_amount"),
                timestamp
        );
    }
}
