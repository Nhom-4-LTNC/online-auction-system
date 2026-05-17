package com.auction.service;

import com.auction.dto.ItemDTO;
import com.auction.exception.InvalidBidException;
import com.auction.model.Bid;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionObserver;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.model.user.User;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Service cho nghiệp vụ đấu giá.
 *
 * <p>Trách nhiệm chính (SRP — chỉ điều phối luồng nghiệp vụ):
 * <ul>
 *   <li>Duy trì cache in-memory ({@code auctions}) để truy cập nhanh.</li>
 *   <li>Điều phối: tạo phiên, đặt giá, thông báo Observer.</li>
 *   <li>Đồng bộ dữ liệu xuống DB qua {@link AuctionRepository} và {@link ItemRepository}.</li>
 * </ul>
 * Validation đầu vào được uỷ quyền cho {@link AuctionValidator}.</p>
 *
 * <p>Luồng dữ liệu: Controller → AuctionService → Repository → Database</p>
 */
public class AuctionService {

    private static volatile AuctionService instance;

    /** Cache in-memory: key = auction ID, truy cập O(1). */
    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    /** Danh sách observer nhận thông báo khi trạng thái phiên thay đổi. */
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private final AuctionRepository auctionRepository = AuctionRepository.getInstance();
    private final ItemRepository    itemRepository    = ItemRepository.getInstance();

    private AuctionService() {
        try {
            for (Auction auction : auctionRepository.getAllAuctions()) {
                auctions.put(auction.getId(), auction);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService] Không thể tải dữ liệu phiên đấu giá từ DB: " + e.getMessage());
        }
    }

    /**
     * Trả về instance duy nhất của {@code AuctionService} (double-checked locking).
     *
     * @return instance singleton của {@code AuctionService}
     */
    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) instance = new AuctionService();
            }
        }
        return instance;
    }



    /**
     * Tạo một phiên đấu giá mới và lưu vào cơ sở dữ liệu.
     *
     * <p>Validation được uỷ quyền cho {@link AuctionValidator}. Thứ tự thực hiện:
     * <ol>
     *   <li>Chuẩn hoá {@code startTimeMillis} (mặc định = now nếu {@code <= 0}).</li>
     *   <li>Validate tham số qua {@link AuctionValidator#validateAuctionParams}.</li>
     *   <li>Tạo {@link Item} qua {@link ItemFactory} với ID tạm {@code 0}.</li>
     *   <li>Validate Item qua {@link AuctionValidator#validateItem}.</li>
     *   <li>Lưu Item vào DB — {@link ItemRepository#addItem} gán ID thật vào object.</li>
     *   <li>Tạo {@link Auction} từ Item đã có ID thật.</li>
     *   <li>Lưu Auction vào DB — {@link AuctionRepository#addAuction} gán ID thật vào object.</li>
     *   <li>Đưa vào cache in-memory.</li>
     * </ol>
     * </p>
     *
     * @param seller          người bán (đã xác thực); không được {@code null}
     * @param itemDto         DTO chứa dữ liệu sản phẩm (ElectronicsDTO, ArtDTO, VehicleDTO)
     * @param bidStep         bước giá tối thiểu; phải lớn hơn {@code 0}
     * @param startTimeMillis thời điểm bắt đầu (ms epoch); truyền {@code 0} để dùng thời gian hiện tại
     * @param endTimeMillis   thời điểm kết thúc (ms epoch); phải lớn hơn {@code startTimeMillis}
     * @return đối tượng {@link Auction} vừa được tạo và lưu
     * @throws IllegalArgumentException nếu vi phạm quy tắc nghiệp vụ
     * @throws Exception                nếu xảy ra lỗi DB
     */
    public synchronized Auction createAuction(User seller, ItemDTO itemDto,
                                              double bidStep,
                                              long startTimeMillis,
                                              long endTimeMillis) throws Exception {
        if (startTimeMillis <= 0) startTimeMillis = System.currentTimeMillis();

        // Validation: uỷ quyền toàn bộ cho AuctionValidator
        AuctionValidator.validateAuctionParams(startTimeMillis, endTimeMillis, bidStep);

        Item newItem = ItemFactory.createItem(itemDto, seller, 0);
        AuctionValidator.validateItem(newItem);

        // Lưu Item vào DB → ID thật được set vào newItem
        itemRepository.addItem(newItem);
        seller.getSellerProfile().addItem(newItem);

        // Tạo Auction dùng Item đã có ID thật, lưu DB → ID thật được set vào newAuction
        Auction newAuction = new Auction(newItem, bidStep, startTimeMillis, endTimeMillis);
        auctionRepository.addAuction(newAuction);

        auctions.put(newAuction.getId(), newAuction);
        return newAuction;
    }

    /**
     * Xử lý một lượt đặt giá cho phiên đấu giá.
     *
     * <p>Thao tác được đồng bộ hóa trên đối tượng {@link Auction} để tránh race condition
     * khi nhiều người cùng đặt giá đồng thời.</p>
     *
     * @param auctionId ID của phiên đấu giá
     * @param bidder    người tham gia đặt giá
     * @param amount    số tiền đặt giá
     * @throws InvalidBidException nếu giá đặt không hợp lệ (quá thấp, chủ item tự đặt, v.v.)
     * @throws Exception           nếu không tìm thấy phiên hoặc xảy ra lỗi DB
     */
    public void placeBid(int auctionId, User bidder, double amount)
            throws InvalidBidException, Exception {

        Auction auction = auctions.get(auctionId);
        if (auction == null)
            throw new Exception("Không tìm thấy phiên đấu giá với ID: " + auctionId);

        synchronized (auction) {
            Bid txn = auction.placeBid(bidder, amount);
            auctionRepository.updateAuction(auction);
        }
    }

    /**
     * Trả về danh sách tất cả phiên đấu giá hiện có trong cache.
     *
     * @return danh sách {@link Auction}; không bao giờ {@code null}
     */
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    /**
     * Tìm phiên đấu giá theo ID trong cache in-memory.
     *
     * @param id ID của phiên đấu giá cần tìm
     * @return đối tượng {@link Auction} tương ứng
     * @throws Exception nếu không tìm thấy phiên với ID đã cho
     */
    public Auction getAuctionById(int id) throws Exception {
        Auction auction = auctions.get(id);
        if (auction == null)
            throw new Exception("Không tìm thấy phiên đấu giá với ID: " + id);
        return auction;
    }

    /**
     * Đăng ký một observer để nhận thông báo khi trạng thái phiên đấu giá thay đổi.
     *
     * @param observer observer cần đăng ký
     */
    public void addObserver(AuctionObserver observer)    { observers.add(observer); }

    /**
     * Huỷ đăng ký một observer.
     *
     * @param observer observer cần huỷ
     */
    public void removeObserver(AuctionObserver observer) { observers.remove(observer); }
}
