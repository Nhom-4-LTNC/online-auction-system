package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository xử lý các thao tác CRUD đối với bảng {@code auctions} trong cơ sở dữ liệu.
 *
 * <p>Lớp này được triển khai theo mẫu Singleton thread-safe (double-checked locking)
 * để đảm bảo chỉ tồn tại một instance duy nhất trong toàn bộ ứng dụng.</p>
 */
public class AuctionRepository {
    private static volatile AuctionRepository instance;

    private AuctionRepository() {}

    /**
     * Trả về instance duy nhất của {@code AuctionRepository}.
     *
     * <p>Sử dụng double-checked locking để đảm bảo an toàn trong môi trường đa luồng.</p>
     *
     * @return instance singleton của {@code AuctionRepository}
     */
    public static AuctionRepository getInstance() {
        if (instance == null) {
            synchronized (AuctionRepository.class) {
                if (instance == null) instance = new AuctionRepository();
            }
        }
        return instance;
    }

    /**
     * Thêm một phiên đấu giá mới vào cơ sở dữ liệu.
     *
     * <p>Sau khi INSERT thành công, ID được sinh tự động từ database sẽ được
     * gán lại vào đối tượng {@code auction} qua {@link Auction#setId(int)}.</p>
     *
     * @param auction đối tượng phiên đấu giá cần lưu; không được {@code null}
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL trong quá trình thêm
     */
    public void addAuction(Auction auction) throws Exception {
        String sql = "INSERT INTO auctions (item_id, starting_price, bid_step, current_price, " +
                "last_bidder_id, start_time, end_time, status, winner_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            /*
            Auction ID,
            Start Price,
            Bid Step,
            Current Price,
            Last Bidder,

            Auction StartTime,
            Auction EndTime,
            Auction Status

            Winner ID (defeault null)
             */

            pstmt.setInt(1, auction.getItem().getId());
            pstmt.setDouble(2, auction.getStartPrice());
            pstmt.setDouble(3, auction.getBidStep());
            pstmt.setDouble(4, auction.getCurrentPrice());

            if (auction.getLastBidder() != null) {
                pstmt.setInt(5, auction.getLastBidder().getId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }

            pstmt.setTimestamp(6, new Timestamp(auction.getStartTime()));
            pstmt.setTimestamp(7, new Timestamp(auction.getEndTime()));
            pstmt.setString(8, auction.getStatus().name());
            pstmt.setNull(9, Types.INTEGER);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Lệnh INSERT không thêm được dòng nào.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auction.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionRepository - addAuction] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi tạo phiên đấu giá: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật thông tin của một phiên đấu giá trong cơ sở dữ liệu.
     *
     * <p>Các trường được cập nhật bao gồm: {@code current_price}, {@code last_bidder_id},
     * {@code status} và {@code winner_id}. Thường được gọi khi có lượt đặt giá mới
     * hoặc khi phiên đấu giá kết thúc.</p>
     *
     * @param auction đối tượng phiên đấu giá chứa dữ liệu mới nhất; ID phải hợp lệ
     * @throws Exception nếu không tìm thấy phiên đấu giá tương ứng, hoặc xảy ra lỗi SQL
     */
    public void updateAuction(Auction auction) throws Exception {
        String sql = "UPDATE auctions SET current_price = ?, last_bidder_id = ?, status = ?, winner_id = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, auction.getCurrentPrice());

            if (auction.getLastBidder() != null) {
                pstmt.setInt(2, auction.getLastBidder().getId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }

            pstmt.setString(3, auction.getStatus().name());

            if (auction.getWinner() != null) {
                pstmt.setInt(4, auction.getWinner().getId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }

            pstmt.setInt(5, auction.getId());

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Không tìm thấy phiên đấu giá với ID = " + auction.getId() + " để cập nhật.");
            }

        } catch (SQLException e) {
            System.err.println("[AuctionRepository - updateAuction] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi cập nhật phiên đấu giá: " + e.getMessage(), e);
        }
    }

    /**
     * Truy vấn và trả về toàn bộ phiên đấu giá trong cơ sở dữ liệu.
     *
     * <p>Với mỗi bản ghi, phương thức tự động load {@link Item} qua {@link ItemRepository}
     * và {@link User} người đặt giá cuối cùng qua {@link UserRepository}.</p>
     *
     * @return danh sách tất cả {@link Auction}; trả về danh sách rỗng nếu bảng không có dữ liệu
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL
     */
    public List<Auction> getAllAuctions() throws Exception {
        String sql = "SELECT * FROM auctions";
        List<Auction> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                Item item = ItemRepository.getInstance().getItemById(itemId);

                Auction auction = new Auction(
                        rs.getInt("id"),
                        item,
                        rs.getDouble("bid_step"),
                        rs.getTimestamp("start_time").getTime(),
                        rs.getTimestamp("end_time").getTime()
                );

                auction.setCurrentPrice(rs.getDouble("current_price"));

                int lastBidderId = rs.getInt("last_bidder_id");
                if (!rs.wasNull()) {
                    User lastBidder = UserRepository.getInstance().getUserById(lastBidderId);
                    auction.setLastBidder(lastBidder);
                }

                list.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionRepository - getAllAuctions] Lỗi: " + e.getMessage());
            throw new Exception("Không thể tải danh sách phiên đấu giá: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Truy vấn và trả về phiên đấu giá theo ID.
     *
     * <p>Nếu tìm thấy bản ghi, phương thức sẽ tự động load đối tượng {@link Item}
     * liên quan qua {@link ItemRepository} và {@link User} người đặt giá cuối cùng
     * qua {@link UserRepository}.</p>
     *
     * @param id ID của phiên đấu giá cần tìm
     * @return đối tượng {@link Auction} tương ứng, hoặc {@code null} nếu không tồn tại
     * @throws Exception nếu xảy ra lỗi kết nối hoặc lỗi SQL trong quá trình truy vấn
     */
    public Auction getAuctionById(int id) throws Exception {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    Item item = ItemRepository.getInstance().getItemById(itemId);

                    Auction auction = new Auction(
                            rs.getInt("id"),
                            item,
                            rs.getDouble("bid_step"),
                            rs.getTimestamp("start_time").getTime(),
                            rs.getTimestamp("end_time").getTime()
                    );

                    auction.setCurrentPrice(rs.getDouble("current_price"));

                    int lastBidderId = rs.getInt("last_bidder_id");
                    if (!rs.wasNull()) {
                        User lastBidder = UserRepository.getInstance().getUserById(lastBidderId);
                        auction.setLastBidder(lastBidder);
                    }

                    return auction;
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionRepository - getAuctionById] Lỗi: " + e.getMessage());
            throw new Exception("Không thể lấy dữ liệu phiên đấu giá: " + e.getMessage(), e);
        }
        return null;
    }
}