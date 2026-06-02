# Đánh giá Codebase — Hệ thống Đấu giá Trực tuyến

> Tài liệu này mô tả hiện trạng dự án theo ngôn ngữ đơn giản, dành cho người mới tham gia.

---

## 1. Dự án này làm được gì?

Đây là một hệ thống đấu giá trực tuyến theo mô hình **Client–Server**:

- **Client** là giao diện JavaFX — nơi người dùng đăng nhập, tạo sản phẩm, đặt giá.
- **Server** nhận yêu cầu từ nhiều client cùng lúc, xử lý nghiệp vụ và lưu xuống cơ sở dữ liệu MySQL (TiDB Cloud).

### Các chức năng đã hoạt động được

| Chức năng | Trạng thái |
|---|---|
| Đăng nhập (email + mật khẩu) | ✅ Hoàn chỉnh |
| Đăng ký tài khoản | ✅ Hoàn chỉnh |
| Kiểm tra độ mạnh mật khẩu (real-time) | ✅ Hoàn chỉnh |
| Tạo sản phẩm (Electronics / Art / Vehicle) | ✅ Hoàn chỉnh |
| Tạo phiên đấu giá mới | ✅ Hoàn chỉnh |
| Đặt giá (placeBid) có kiểm tra số dư, bước giá | ✅ Hoàn chỉnh |
| Giao tiếp client–server qua mạng (TCP socket) | ✅ Hoàn chỉnh |
| Lưu/đọc dữ liệu từ database | ✅ Hoàn chỉnh |
| Phân quyền người dùng (BIDDER / SELLER / ADMIN) | ✅ Có cấu trúc, chưa kiểm tra đầy đủ |
| Cấm người dùng (ban) | ✅ Có trong service, chưa có UI |
| Thông báo kết quả đấu giá cho tất cả client | ❌ Chưa làm |
| Lịch sử đặt giá hiển thị trên UI | ❌ Chưa làm |
| Thanh toán / kết thúc phiên | ❌ Chưa làm |

---

## 2. Kiến trúc tổng thể

```
[Client - JavaFX]
      |
      | TCP Socket (gửi/nhận object Java được serialize)
      |
[Server]
   |-- ClientHandler (mỗi client 1 thread)
   |-- AuctionService / UserService / ItemService
   |-- Repository (đọc/ghi database)
   |-- MySQL Database (TiDB Cloud)
```

**Luồng dữ liệu điển hình (ví dụ: đăng nhập):**
1. Client nhập email + mật khẩu → bấm nút Login
2. Controller gửi `AuthRequest` qua socket đến Server
3. `ClientHandler` trên Server nhận → gọi `UserService.login()`
4. `UserService` → `UserRepository` → truy vấn MySQL
5. Server trả về `AuthResponse` (thành công / thất bại)
6. Controller nhận phản hồi → chuyển sang màn hình chính

---

## 3. Chi tiết từng tầng

### Tầng Model (`model/`)
Chứa các lớp dữ liệu thuần tuý — không kết nối database, không giao tiếp mạng.

| Lớp | Mô tả |
|---|---|
| `Entity` | Lớp cha của mọi đối tượng, có `id` và `createdAt` |
| `User` | Người dùng, có thể là Bidder / Seller / Admin cùng lúc |
| `BidderProfile` | Quản lý số dư tài khoản, kiểm tra `canAfford()` |
| `SellerProfile` | Danh sách sản phẩm của người bán |
| `Item` (abstract) | Sản phẩm đấu giá — 3 loại: `Electronics`, `Art`, `Vehicle` |
| `Auction` | Phiên đấu giá, chứa lịch sử bid và trạng thái tự động tính |
| `BidTransaction` | Một lượt đặt giá (ai, bao nhiêu, lúc nào) |

**Lưu ý về `Auction`:** Trạng thái (`OPEN` / `RUNNING` / `FINISHED`) được tính tự động dựa vào thời gian thực — không cần set thủ công.

---

### Tầng Repository (`repository/`)
Chịu trách nhiệm duy nhất: **đọc/ghi database**. Không chứa logic nghiệp vụ.

| Lớp | Bảng DB |
|---|---|
| `UserRepository` | `users` |
| `ItemRepository` | `items` |
| `AuctionRepository` | `auctions` |

Cả 3 đều dùng **Singleton** (một instance duy nhất cho cả app) và **double-checked locking** để an toàn đa luồng.

---

### Tầng Service (`service/`)
Chứa logic nghiệp vụ. Controller **không** được gọi Repository trực tiếp — phải qua Service.

| Lớp | Trách nhiệm |
|---|---|
| `UserService` | Đăng nhập, đăng ký, cấm user, tìm kiếm |
| `AuctionService` | Tạo phiên, đặt giá, cache phiên trong bộ nhớ |
| `ItemService` | Tạo sản phẩm, truy vấn theo chủ sở hữu |
| `AuctionValidator` | Kiểm tra tham số đầu vào (tách riêng theo SRP) |

**`AuctionService` giữ một cache `Map<Integer, Auction>`** để truy cập nhanh — không phải query DB mỗi lần.

---

### Tầng Protocol (`protocol/`)
Định nghĩa các gói tin gửi qua mạng. Mọi lớp đều implement `Serializable`.

```
Client  →  AuthRequest / CreateAuctionRequest  →  Server
Client  ←  AuthResponse / AuctionResponse      ←  Server
```

| Lớp | Chiều | Dùng cho |
|---|---|---|
| `AuthRequest` | Client → Server | Đăng nhập, đăng ký |
| `AuthResponse` | Server → Client | Kết quả đăng nhập/đăng ký |
| `CreateAuctionRequest` | Client → Server | Tạo phiên đấu giá |
| `AuctionResponse` | Server → Client | Kết quả tạo phiên, đặt giá |
| `BidMessage` | Server → tất cả Client | Thông báo có bid mới |
| `ActionType` | — | Enum phân loại tất cả các loại hành động |

---

### Tầng Client (`client/`)
Giao diện JavaFX. Mỗi màn hình có một Controller tương ứng.

| Controller | Màn hình |
|---|---|
| `LoginController` | Đăng nhập |
| `CreateAccountController` | Đăng ký tài khoản |
| `AuctionMenuController` | Danh sách đấu giá sau khi user đăng nhập |
| `AdminMenuController` | Màn hình quản trị sau khi admin đăng nhập |
| `AuctionItemMenuController` | Tạo sản phẩm và phiên đấu giá |

**Quy tắc quan trọng:** Controller **không** gọi Service trực tiếp. Thay vào đó:
1. Đăng ký callback: `client.setOnMessageReceived(response -> { ... })`
2. Gửi request: `client.sendMessage(new SomeRequest(...))`
3. Xử lý khi server trả về trong callback

---

### Tầng Server (`server/`)

| Lớp | Vai trò |
|---|---|
| `Server` | Lắng nghe kết nối mới, quản lý danh sách client |
| `ClientHandler` | Mỗi client được phục vụ bởi 1 `ClientHandler` chạy trên 1 thread riêng |

**`Server.broadcast()`** dùng để gửi thông báo đến tất cả client (ví dụ: có bid mới).

---

## 4. Những chỗ cần lưu ý

### Vấn đề nghiêm trọng cần xử lý trước khi nộp / deploy

#### ⚠️ Credential database bị hardcode trong code
**File:** `config/DatabaseConnection.java`

Username và password của TiDB Cloud đang được viết thẳng vào code. Nếu project được đẩy lên GitHub public, tài khoản database sẽ bị lộ.

**Cách sửa:** Chuyển sang đọc từ file `.env` hoặc biến môi trường, rồi thêm `.env` vào `.gitignore`.

---

#### ⚠️ Mật khẩu lưu dạng plain-text
**File:** `UserRepository.java`, `UserService.java`

Mật khẩu người dùng được lưu thẳng xuống DB mà không qua băm (hash). Nếu DB bị lộ, toàn bộ mật khẩu đều bị đọc được.

**Cách sửa:** Dùng `BCrypt` để hash trước khi lưu, kiểm tra bằng `BCrypt.checkpw()` khi đăng nhập.

---

### Vấn đề thiết kế cần dọn dẹp

#### File protocol bị trùng lặp
Có hai phiên bản của `AuthRequest` và `AuthResponse`:
- `com.auction.protocol.AuthRequest` ← cái này đang được dùng
- `com.auction.protocol.auth.AuthRequest` ← bản copy không dùng đến

Tương tự với `CreateAuctionRequest` ở `protocol/` (bản rỗng) và `protocol/auction/` (bản đầy đủ).

**Cách sửa:** Xoá các file trùng hoặc rỗng trong thư mục `protocol/` gốc.

---

#### Số dư người đặt giá chưa bị trừ
**File:** `Auction.java`, method `placeBid()`

Khi ai đó đặt giá thành công, code ghi nhận bid nhưng **không trừ tiền** khỏi tài khoản. `BidderProfile.withdraw()` đã có sẵn nhưng chưa được gọi.

---

#### Observer pattern đăng ký nhưng không dùng
**File:** `AuctionService.java`

`addObserver()` / `removeObserver()` đã có, nhưng không có chỗ nào trong code gọi `observer.onNewBidPlace()`. Kết quả: đặt giá xong, không ai được thông báo.

---

#### `UserService.findByUsername()` tải toàn bộ user
**File:** `UserService.java`

Hàm tìm kiếm user theo username đang load **tất cả user** từ DB rồi lọc bằng Java. `UserRepository` đã có sẵn `getUserByUsername()` dùng SQL `WHERE username = ?` nhưng chưa được gọi.

---

#### `GetAuctionRequest` và `PlaceBidRequest` còn rỗng
**File:** `protocol/auction/GetAuctionRequest.java`, `PlaceBidRequest.java`

Hai lớp này chỉ có khai báo class, không có nội dung. Chức năng "xem danh sách phiên" và "đặt giá qua mạng" chưa hoàn chỉnh.

---

### Vấn đề nhỏ hơn (không ảnh hưởng chức năng ngay)

| Vị trí | Vấn đề |
|---|---|
| `Server.java` | Typo biến `clienSocket` (thiếu chữ 't') |
| `Item.toString()` | `String.format()` có 6 placeholder nhưng truyền 7 tham số → lỗi runtime |
| `SessionManager.getInstance()` | Không thread-safe (thiếu `synchronized`) |
| `LoginController` + `AuctionItemMenuController` | Mỗi lần gửi request lại ghi đè `onMessageReceived` — nếu server trả về nhiều phản hồi liên tiếp có thể bị nhầm handler |
| `NetworkConfig` | `SERVER_IP = "127.0.0.1"` — chỉ chạy được trên máy cục bộ |

---

## 5. Những gì còn thiếu để hoàn chỉnh

```
[ ] Kết thúc phiên đấu giá tự động khi hết giờ
[ ] Trừ tiền người thắng, hoàn tiền người thua
[ ] Hiển thị danh sách phiên đang diễn ra trên UI
[ ] Hiển thị lịch sử đặt giá trong phiên
[ ] Thông báo real-time cho client khi có bid mới (broadcast)
[ ] Màn hình quản trị Admin (cấm user, xem thống kê)
[ ] Tìm kiếm / lọc sản phẩm
[ ] Nạp tiền vào tài khoản Bidder
[ ] Lưu lịch sử bid vào bảng `bid_transactions` trong DB
```

---

## 6. Tóm tắt nhanh

**Điểm mạnh của codebase:**
- Kiến trúc rõ ràng: Model → Repository → Service → Controller
- Giao tiếp mạng đúng hướng (client không gọi thẳng service)
- Singleton + double-checked locking nhất quán
- SRP được áp dụng tốt (AuctionValidator tách riêng)
- JavaDoc đầy đủ ở các tầng Repository và Service

**Điểm cần cải thiện ngay:**
1. Xử lý credential database (bảo mật)
2. Hash mật khẩu trước khi lưu
3. Trừ tiền khi đặt giá thành công
4. Xoá file protocol trùng lặp
5. Hoàn thiện `PlaceBidRequest` và `GetAuctionRequest`
