package com.auction.server.service;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.exception.ValidationException;

import java.sql.Connection;
import java.sql.SQLException;

public class WalletService {
    private static volatile WalletService instance;

    private final UserRepository userRepository = UserRepository.getInstance();
    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();

    private WalletService() {}

    public static WalletService getInstance() {
        if (instance == null) {
            synchronized (WalletService.class) {
                if (instance == null) instance = new WalletService();
            }
        }
        return instance;
    }

    public BalanceResponse addBalance(int userId, double amount) throws Exception {
        if (amount <= 0) {
            throw new ValidationException("Số tiền nạp phải lớn hơn 0.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                userRepository.addUserBalance(conn, userId, amount);
                BalanceResponse walletSummary = getWalletSummary(conn, userId);

                conn.commit();
                return new BalanceResponse(
                        walletSummary.getBalance(),
                        walletSummary.getUnpaidWinningAmount(),
                        walletSummary.getAvailableBalance(),
                        "Nạp tiền thành công."
                );
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }
    }

    public double getAvailableBalance(Connection conn, int userId) throws Exception {
        return getWalletSummary(conn, userId).getAvailableBalance();
    }

    public double getAvailableBalanceForBid(Connection conn, int userId, int auctionId) throws Exception {
        double balance = userRepository.getUserBalanceForUpdate(conn, userId);
        double unpaidWinningAmount = auctionRepository.getUnpaidWinningAmount(conn, userId);
        double activeLeadingAmountExceptCurrent =
                auctionRepository.getActiveLeadingAmountExcludingAuction(conn, userId, auctionId);

        return balance - unpaidWinningAmount - activeLeadingAmountExceptCurrent;
    }

    public BalanceResponse getWalletSummary(Connection conn, int userId) throws Exception {
        double balance = userRepository.getUserBalance(conn, userId);
        double unpaidWinningAmount = auctionRepository.getUnpaidWinningAmount(conn, userId);
        double activeLeadingAmount = auctionRepository.getActiveLeadingAmountExcludingAuction(conn, userId, 0);
        double availableBalance = balance - unpaidWinningAmount - activeLeadingAmount;

        return new BalanceResponse(
                balance,
                unpaidWinningAmount,
                availableBalance,
                "Lấy thông tin ví thành công."
        );
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[WalletService] Cannot rollback transaction: " + rollbackError.getMessage());
        }
    }

    private void restoreAutoCommitQuietly(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreError) {
            System.err.println("[WalletService] Cannot restore autoCommit: " + restoreError.getMessage());
        }
    }
}
