package com.auction.server.model.auction;

import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.server.model.Bid;
import com.auction.server.model.Entity;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;

import java.io.Serial;

public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 6720930536578062003L;

    private Item item;
    private long startTime;
    private long endTime;
    private double startPrice;
    private double currentPrice;
    private User lastBidder;
    private User winner;
    private double bidStep;
    private AuctionStatus status;

    public Auction(Item item, double bidStep, long startTime, long endTime) {
        super();
        this.item = item;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPrice = item.getStartPrice();
        this.currentPrice = startPrice;
        updateStatus();
    }

    public Auction(int id, Item item, double bidStep, long startTime, long endTime) {
        super(id);
        this.item = item;
        this.startPrice = item.getStartPrice();
        this.currentPrice = startPrice;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        updateStatus();
    }

    /**
     * Applies the in-memory domain bidding rules.
     *
     * <p>Only RUNNING auctions are biddable. The seller cannot bid on their own
     * item, and a later bid must respect the configured bid step. Persistence,
     * wallet checks and realtime notifications are handled by BidService.</p>
     */
    public synchronized Bid placeBid(User bidder, double amount) throws InvalidBidException, AuctionClosedException {
        refreshStatus(System.currentTimeMillis());
        if (!isBiddable()) {
            throw new AuctionClosedException(this.id);
        }
        if (bidder == null) {
            throw new InvalidBidException("Người đặt giá không hợp lệ!");
        }
        if (bidder.getId() == item.getOwner().getId()) {
            throw new InvalidBidException("Lỗi: Không thể đấu giá cho sản phẩm của mình tạo ra!");
        }
        if (lastBidder == null) {
            if (amount < startPrice) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng giá khởi đầu!");
            }
        } else {
            if (amount < currentPrice + bidStep) {
                throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại cộng bước giá!");
            }
        }
        this.currentPrice = amount;
        this.lastBidder = bidder;

        return new Bid(this.id, bidder.getId(), amount);
    }

    public void updateStatus() {
        refreshStatus(System.currentTimeMillis());
    }

    public AuctionStatus getStatus() {
        return status;
    }

    /**
     * Refreshes time-based status using the supplied server time.
     *
     * <p>OPEN can become RUNNING, RUNNING can become FINISHED. FINISHED, PAID
     * and CANCELED are terminal for this domain method and are not moved back.</p>
     */
    public synchronized void refreshStatus(long now) {
        if (status == AuctionStatus.FINISHED
                || status == AuctionStatus.PAID
                || status == AuctionStatus.CANCELED) {
            return;
        }

        if (now < startTime) {
            status = AuctionStatus.OPEN;
            return;
        }

        if (now < endTime) {
            status = AuctionStatus.RUNNING;
            return;
        }

        finishIfNeeded();
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public boolean isBiddable() {
        return this.status == AuctionStatus.RUNNING;
    }

    public synchronized void close() {
        finishIfNeeded();
    }

    private void finishIfNeeded() {
        if (winner == null && lastBidder != null) {
            winner = lastBidder;
        }
        this.status = AuctionStatus.FINISHED;
    }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public User getLastBidder() { return lastBidder; }
    public void setLastBidder(User user) { lastBidder = user; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public double getCurrentPrice() { return this.currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getBidStep() { return bidStep; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    @Override
    public String toString() {
        return String.format("Auction{id=%d, item=%s, currentPrice=%.2f, status=%s}",
                id, item.getName(), currentPrice, status);
    }
}
