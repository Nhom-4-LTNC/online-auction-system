package com.auction.server.service;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.model.auction.Auction;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.BalanceResponse;
import com.auction.shared.dto.PayAuctionResponse;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.exception.AuthorizationException;
import com.auction.shared.exception.ResourceNotFoundException;

import java.sql.Connection;
import java.sql.SQLException;

public class PaymentService {
    private static volatile PaymentService instance;

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final WalletService walletService = WalletService.getInstance();

    private PaymentService() {}

    public static PaymentService getInstance() {
        if (instance == null) {
            synchronized (PaymentService.class) {
                if (instance == null) instance = new PaymentService();
            }
        }
        return instance;
    }

    /**
     * Pays a finished auction in a server-side transaction.
     *
     * <p>The payer must be the winner, the auction must be FINISHED, and PAID
     * auctions are rejected to prevent double payment. Funds move from winner
     * to seller before the auction status is marked PAID.</p>
     */
    public PayAuctionResponse payAuction(int payerId, int auctionId) throws Exception {
        Auction auction;
        double paidAmount;
        double newBalance;
        double newUnpaidWinningAmount;
        double newAvailableBalance;

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                auction = auctionRepository.findByIdForUpdate(conn, auctionId);
                if (auction == null) {
                    throw new ResourceNotFoundException("Phiên đấu giá", auctionId);
                }

                auction.refreshStatus(System.currentTimeMillis());
                if (auction.getStatus() == AuctionStatus.PAID) {
                    throw new AuctionAppException("Phiên đấu giá đã được thanh toán.");
                }
                if (auction.getStatus() != AuctionStatus.FINISHED) {
                    throw new AuctionAppException("Chỉ có thể thanh toán phiên đấu giá đã kết thúc.");
                }
                if (auction.getWinner() == null) {
                    throw new AuctionAppException("Phiên đấu giá không có người thắng.");
                }

                int winnerId = auction.getWinner().getId();
                if (payerId != winnerId) {
                    throw new AuthorizationException("Bạn không có quyền thanh toán phiên đấu giá này.");
                }

                int sellerId = auction.getItem().getOwner().getId();
                paidAmount = auction.getCurrentPrice();

                userRepository.deductUserBalance(conn, winnerId, paidAmount);
                userRepository.addUserBalance(conn, sellerId, paidAmount);

                auction.setStatus(AuctionStatus.PAID);
                auctionRepository.updateAuction(conn, auction);

                BalanceResponse walletSummary = walletService.getWalletSummary(conn, winnerId);
                newBalance = walletSummary.getBalance();
                newUnpaidWinningAmount = walletSummary.getUnpaidWinningAmount();
                newAvailableBalance = walletSummary.getAvailableBalance();

                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                restoreAutoCommitQuietly(conn, originalAutoCommit);
            }
        }

        AuctionService.getInstance().updateCachedAuction(auction);
        return new PayAuctionResponse(
                auctionId,
                paidAmount,
                newBalance,
                newUnpaidWinningAmount,
                newAvailableBalance,
                "Thanh toán phiên đấu giá thành công."
        );
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[PaymentService] Cannot rollback transaction: " + rollbackError.getMessage());
        }
    }

    private void restoreAutoCommitQuietly(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreError) {
            System.err.println("[PaymentService] Cannot restore autoCommit: " + restoreError.getMessage());
        }
    }
}
