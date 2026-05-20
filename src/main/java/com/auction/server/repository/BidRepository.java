package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.Bid;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository chịu trách nhiệm truy cập dữ liệu cho bảng {@code bids}.
 *
 * <p>Lớp này cung cấp các thao tác CRUD và truy vấn liên quan đến:
 * <ul>
 *     <li>Lưu bid mới</li>
 *     <li>Tìm bid theo ID</li>
 *     <li>Lấy danh sách bid của một auction</li>
 *     <li>Lấy bid cao nhất của auction</li>
 *     <li>Lấy lịch sử bid của một bidder</li>
 * </ul>
 *
 * <p>Repository chỉ xử lý thao tác database,
 * không chứa business logic đấu giá.</p>
 *
 * <p>Pattern sử dụng: Singleton.</p>
 */
public class BidRepository {

    /** Singleton instance của BidRepository */
    private static BidRepository instance;

    /**
     * Constructor private để ngăn tạo object trực tiếp.
     */
    private BidRepository() {
    }

    /**
     * Lấy instance duy nhất của {@code BidRepository}.
     *
     * @return singleton instance
     */
    public static synchronized BidRepository getInstance() {
        if (instance == null) {
            instance = new BidRepository();
        }
        return instance;
    }

    /**
     * Lưu một bid mới vào database.
     *
     * @param bid bid cần lưu
     * @return bid sau khi lưu thành công
     * @throws SQLException nếu xảy ra lỗi SQL
     */
    public Bid save(Bid bid) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, bid.getAuctionId());
            stmt.setInt(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Không thể lưu bid vào cơ sở dữ liệu");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getInt(1));
                }
            }

            return bid;

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lưu bid", e);
        }
    }

    /**
     * Tìm bid theo ID.
     *
     * @param id ID của bid
     * @return bid tương ứng
     * @throws RuntimeException nếu không tìm thấy hoặc lỗi SQL xảy ra
     */
    public Bid findById(int id) {
        String sql = "SELECT * FROM bids WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (var rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bid theo ID", e);
        }

        throw new RuntimeException("Không tìm thấy bid với ID: " + id);
    }

    /**
     * Lấy danh sách tất cả bid thuộc một auction.
     *
     * <p>Danh sách được sắp xếp theo thời gian tạo giảm dần
     * (bid mới nhất đứng trước).</p>
     *
     * @param auctionId ID của auction
     * @return danh sách bid
     * @throws RuntimeException nếu xảy ra lỗi SQL
     */
    public List<Bid> findByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time DESC";

        List<Bid> bids = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }

            return bids;

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bid theo auction ID", e);
        }
    }

    /**
     * Lấy bid có giá trị cao nhất của một auction.
     *
     * @param auctionId ID của auction
     * @return bid cao nhất
     * @throws RuntimeException nếu không có bid hoặc xảy ra lỗi SQL
     */
    public Bid findHighestBidByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);

            try (var rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bid cao nhất theo auction ID", e);
        }

        throw new RuntimeException("Không tìm thấy bids cho auction ID: " + auctionId);
    }

    /**
     * Lấy toàn bộ lịch sử bid của một bidder.
     *
     * <p>Danh sách được sắp xếp theo thời gian tạo giảm dần
     * (bid mới nhất đứng trước).</p>
     *
     * @param bidderId ID của bidder
     * @return danh sách bid của bidder
     * @throws RuntimeException nếu không có bid hoặc xảy ra lỗi SQL
     */
    public List<Bid> findByBidderId(int bidderId) {
        String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY bid_time DESC";

        List<Bid> bids = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bidderId);

            try (var rs = stmt.executeQuery()) {

                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }

            if (bids.isEmpty()) {
                throw new RuntimeException("Không tìm thấy bids cho bidder ID: " + bidderId);
            }

            return bids;

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bid theo bidder ID", e);
        }
    }

    /**
     * Chuyển đổi một dòng dữ liệu trong {@link ResultSet}
     * thành object {@link Bid}.
     *
     * @param rs ResultSet chứa dữ liệu bid
     * @return object Bid tương ứng
     * @throws SQLException nếu đọc dữ liệu thất bại
     */
    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        return new Bid(
                rs.getInt("id"),
                rs.getInt("auction_id"),
                rs.getInt("bidder_id"),
                rs.getDouble("bid_amount"),
                rs.getTimestamp("bid_time")
                        .toInstant()
                        .toEpochMilli()
        );
    }
}