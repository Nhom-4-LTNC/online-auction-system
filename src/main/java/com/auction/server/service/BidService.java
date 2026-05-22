package com.auction.server.service;


import com.auction.shared.dto.BidDTO;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.exception.ResourceNotFoundException;
import com.auction.server.model.Bid;
import com.auction.server.model.auction.Auction;
import com.auction.server.model.user.User;
import com.auction.shared.protocol.bid.PlaceBidRequest;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;

import java.util.List;

public class BidService {
    private static volatile BidService instance;

    private final BidRepository bidRepository = BidRepository.getInstance();
    private final UserService userService = UserService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();

    private BidService() {}
    public static BidService getInstance() {
        if (instance == null) {
            synchronized (BidService.class) {
                if (instance == null) instance = new BidService();
            }
        }
        return instance;
    }
    /**
     * Đặt giá cho một phiên đấu giá.
     *
     * @param currentUser user đang đăng nhập, lấy từ ClientHandler
     * @param auctionId id của phiên đấu giá
     * @param amount giá đặt
     * @return Auction đã được cập nhật sau khi đặt giá thành công
     * @throws Exception nếu chưa đăng nhập, không có quyền, auction không tồn tại,
     *                  bid không hợp lệ hoặc lỗi database
     */
    public Auction placeBid(UserDTO currentUser, int auctionId, double amount) throws Exception {
        if (currentUser == null) {
            throw new AuctionAppException("Bạn cần đăng nhập để đặt giá!");
        }

        User bidder = userService.getUserById(currentUser.getId());
        if (bidder == null) {
            throw new ResourceNotFoundException("Người đặt giá", currentUser.getId());
        }

        Auction auction = auctionService.getAuctionModelById(auctionId);

        synchronized (auction) {
            Bid bid = auction.placeBid(bidder, amount);
            bidRepository.save(bid);
            auctionRepository.updateAuction(auction);
        }
        return auction;
    }

    /**
     * Lấy danh sách các bid theo id của đấu giá
     * @param auctionId
     * @return
     */
    public List<Bid> getBidsByAuctionId(int auctionId) throws Exception {
        return bidRepository.findByAuctionId(auctionId);
    }

    /**
     * Lấy bid cao nhất của một auction
     * @param auctionId
     * @return
     */
    public Bid getHighestBidByAuctionId(int auctionId) throws Exception {
        return bidRepository.findHighestBidByAuctionId(auctionId);
    }

    /**
     * Lấy danh sách các bid của một người đặt giá
     * @param bidderId
     * @return
     */
    public List<Bid> getBidsByBidder(int bidderId) throws Exception {
        return bidRepository.findByBidderId(bidderId);
    }

    /**
     * Map Bid model sang BidDTO
     * @param bid
     * @return
     */
    public BidDTO mapToBidDTO(Bid bid) throws AuctionAppException {
        User bidder = null;
        try {
            bidder = userService.getUserById(bid.getBidderId());
        } catch (Exception e) {
            throw new AuctionAppException("Lỗi khi lấy thông tin người đặt giá");
        }

        String BidderUsername = bidder != null
                ? bidder.getUsername()
                : "Unknown";

        return new BidDTO(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                BidderUsername,
                bid.getAmount(),
                bid.getTimestamp()
        );
    }

    public List<BidDTO> mapToBidDTOList(List<Bid> bids) throws AuctionAppException {
        return bids.stream()
                .map(bid -> {
                    try {
                        return mapToBidDTO(bid);
                    } catch (Exception e) {
                        try {
                            throw new AuctionAppException("Không thể map Bid sang BidDTO");
                        } catch (AuctionAppException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                })
                .toList();
    }

}
