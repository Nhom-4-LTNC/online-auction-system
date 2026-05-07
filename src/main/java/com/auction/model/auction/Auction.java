package com.auction.model.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientFundsException;
import com.auction.exception.InvalidBidException;
import com.auction.model.BidTransaction;
import com.auction.model.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.BidderProfile;
import com.auction.model.user.User;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    @Serial
    private static final long serialVersionUID = 6720930536578062003L;
    private Item item;
    private long startTime;
    private long endTime;
    private double startPrice;
    private double currentPrice;
    private User lastBidder;
    private final double bidStep;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    // CONSTRUCTORS
    public Auction(Item item, double bidStep, long startTime, long endTime) {
        super();
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPrice = item.getStartPrice();
        this.currentPrice = startPrice;
    }
    public Auction(int id, Item item, double bidStep, long startTime, long endTime) {
        super(id);
        this.item = item;
        this.startPrice = item.getStartPrice();
        this.currentPrice = startPrice;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // METHODS
    public synchronized BidTransaction placeBid(User user, double amount) throws InvalidBidException, AuctionClosedException, InsufficientFundsException {

        // THIS CREATES A NEW BidTransaction with AuctionID, UserID, amount

        if (getStatus() != AuctionStatus.OPENED) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng hoặc chưa mở!");
        }
        BidderProfile profile = user.getBidderProfile();
        if (user.getId() == item.getOwnerId()) {
            throw new InvalidBidException("Lỗi: Không thể đấu giá cho sản phẩm của mình tạo ra!");
        }
        if (lastBidder == null && amount < startPrice) {
            throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng giá khởi đầu!");
        } else if (amount < currentPrice + bidStep) {
            throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại cộng bước giá!");
        }
        if (!profile.canAfford(amount)) {
            throw new InsufficientFundsException("Số dư tài khoản không đủ!");
        }

        this.currentPrice = amount;
        this.lastBidder = user;

        BidTransaction bidTransaction = new BidTransaction(this.id, user.getId(), user.getUsername(), amount);
        this.bidHistory.add(bidTransaction);

        return bidTransaction;

    }
    //STATUS UPDATE
    public AuctionStatus getStatus() {
        long now = System.currentTimeMillis();
        if (now < startTime) return AuctionStatus.INITIALIZED; // not opened yet
        else if (now > endTime) return AuctionStatus.CLOSED; // past closing time
        return AuctionStatus.OPENED; // is open right now
    }
    //GET WINNER
    public User getWinner() {
        if (System.currentTimeMillis() > endTime || getStatus() == AuctionStatus.CLOSED) {
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

    public List <BidTransaction> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }
}
