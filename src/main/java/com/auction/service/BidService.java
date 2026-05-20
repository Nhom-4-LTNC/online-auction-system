package com.auction.service;


import com.auction.dto.BidDTO;
import com.auction.dto.UserDTO;
import com.auction.exception.AuctionAppException;
import com.auction.exception.ResourceNotFoundException;
import com.auction.model.Bid;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.protocol.bid.PlaceBidRequest;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;

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
     * @param request payload từ client
     * @return Auction đã được cập nhật sau khi đặt giá thành công
     * @throws Exception nếu chưa đăng nhập, không có quyền, auction không tồn tại,
     *                  bid không hợp lệ hoặc lỗi database
     */
    public Auction placeBid(UserDTO currentUser, PlaceBidRequest request) throws Exception {
        if (currentUser == null) {
            throw new AuctionAppException("Bạn cần đăng nhập để đặt giá!");
        }

        User bidder = userService.getUserById(currentUser.getId());
        if (bidder == null) {
            throw new ResourceNotFoundException("Người đặt giá", currentUser.getId());
        }

        Auction auction = auctionService.getAuctionModelById(request.getAuctionId());

        synchronized (auction) {
            Bid bid =   auction.placeBid(bidder, request.getAmount());

            Bid savedBid = bidRepository.save(bid);
            auctionRepository.updateAuction(auction);

            return auction;
        }
    }

    /**
     * Lấy danh sách các bid theo id của đấu giá
     * @param bidId
     * @return
     */
    public List<Bid> getBidsByAuctionId(int bidId) {
        return bidRepository.findByAuctionId(bidId);
    }

    /**
     * Lấy bid cao nhất của một auction
     * @param auctionId
     * @return
     */
    public Bid getHighestBidByAuctionId(int auctionId) {
        return bidRepository.findHighestBidByAuctionId(auctionId);
    }

    /**
     * Lấy danh sách các bid của một người đặt giá
     * @param bidderId
     * @return
     */
    public List<Bid> getBidsByBidder(int bidderId) {
        return bidRepository.findByBidderId(bidderId);
    }

    /**
     * Map Bid model sang BidDTO
     * @param bid
     * @return
     */
    public BidDTO mapToBidDTO(Bid bid) {
        User bidder = null;
        try {
            bidder = userService.getUserById(bid.getBidderId());
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public List<BidDTO> mapToBidDTOList(List<Bid> bids) {
        return bids.stream()
                .map(bid -> {
                    try {
                        return mapToBidDTO(bid);
                    } catch (Exception e) {
                        throw new RuntimeException("Không thể map Bid sang BidDTO", e);
                    }
                })
                .toList();
    }

}
