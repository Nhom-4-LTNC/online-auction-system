package com.auction.server.model.auction;

import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InsufficientFundsException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.server.model.Bid;
import com.auction.server.model.Entity;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.BidderProfile;
import com.auction.server.model.user.User;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 6720930536578062003L;

    public static double DEFAULT_BID_STEP = 0.1;

    private Item item;
    private long startTime;
    private long endTime;
    private double startPrice;
    private double currentPrice;
    private User lastBidder;
    private double bidStep;

    private final List<Bid> bidHistory = new ArrayList<>();
    private AuctionStatus status;

    // CONSTRUCTORS
    public Auction(Item item, double bidStep, long startTime, long endTime) {
        super();
        if (bidStep < DEFAULT_BID_STEP) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn hoặc bằng " + DEFAULT_BID_STEP);
        }
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

    // METHODS
    public synchronized Bid placeBid(User user, double amount) throws InvalidBidException, AuctionClosedException, InsufficientFundsException {
        if (getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(this.id);
        }
        BidderProfile profile = user.getBidderProfile();
        if (user.getId() == item.getOwnerId()) {
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
        if (!profile.canAfford(amount)) {
            throw new InsufficientFundsException();
        }

        this.currentPrice = amount;
        this.lastBidder = user;

        Bid bidTransaction = new Bid(this.id, user.getId(), amount);
        this.bidHistory.add(bidTransaction);

        return bidTransaction;

    }
    // Cập nhật hàm updateStatus() trong Auction.java
    public void updateStatus() {
        long now = System.currentTimeMillis();
        // Không ghi đè nếu trạng thái đã là PAID hoặc CANCELED (vì đây là trạng thái thủ công)
        if (this.status == AuctionStatus.PAID || this.status == AuctionStatus.CANCELED) {
            return;
        }

        if (now < startTime) {
            this.status = AuctionStatus.OPEN; // Đang chờ tới giờ
        } else if (now >= startTime && now <= endTime) {
            this.status = AuctionStatus.RUNNING; // Đang diễn ra
        } else {
            this.status = AuctionStatus.FINISHED; // Đã kết thúc
        }
    }
    public AuctionStatus getStatus() {
        updateStatus();
        return status;
    }
    public void setStatus(AuctionStatus status) { this.status = status; }
    //GET WINNER
    public User getWinner() {
        if (System.currentTimeMillis() > endTime || getStatus() == AuctionStatus.FINISHED) {
                return lastBidder;
        }
        return null;
    }

    //GETTER, SETTER
    public User getLastBidder() {
        return lastBidder;
    }
    public void setLastBidder(User user) { lastBidder = user; }
    public double getStartPrice() {
        return startPrice;
    }
    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }
    public double getCurrentPrice() {return this.currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getBidStep() { return bidStep; }
    public void setBidStep(double bidStep) {
        if (bidStep >= DEFAULT_BID_STEP) {
            this.bidStep = bidStep;
        }
    }

    public long getEndTime() {
        return endTime;
    }
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    public List <Bid> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }
    @Override
    public String toString() {
        return String.format("Auction{id=%d, item=%s, currentPrice=%.2f, status=%s}",
                id, item.getName(), currentPrice, status);
    }
}
