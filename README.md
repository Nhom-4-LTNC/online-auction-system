# Online Auction System

Online Auction System là ứng dụng đấu giá trực tuyến dạng desktop theo kiến trúc **Client-Server**. Hệ thống cho phép người dùng đăng ký, đăng nhập, tạo phiên đấu giá, đặt giá, theo dõi cập nhật realtime, nạp tiền, thanh toán phiên đã thắng và xem lịch sử đặt giá. Admin có thể quản lý người dùng và xem chi tiết các phiên đấu giá.

Server chịu trách nhiệm xử lý nghiệp vụ, transaction và truy cập database. JavaFX client chỉ giao tiếp với server thông qua socket bằng các đối tượng `Request` / `Response`.

---

## 1. Công nghệ sử dụng

* Java 21
  * JavaFX + FXML
  * Java Socket với `ObjectInputStream` / `ObjectOutputStream`
  * JDBC
  * MySQL hoặc TiDB Cloud
  * HikariCP
  * Maven / Maven Wrapper
  * JUnit 5
  * BCrypt

---

## 2. Yêu cầu cài đặt

Cần cài đặt:

* JDK 21 hoặc phiên bản tương thích với project
  * MySQL hoặc TiDB Cloud
  * Maven, hoặc dùng Maven Wrapper có sẵn trong project
  * IntelliJ IDEA hoặc IDE Java tương đương

Kiểm tra môi trường:

```bash
java -version
mvn -version
```

Cấu hình database nằm tại:

```text
src/main/resources/db.properties
```

Nếu có file mẫu, sao chép từ:

```text
src/main/resources/db.properties.example
```

Ví dụ:

```properties
db.url=jdbc:mysql://localhost:3306/auction_db
db.username=root
db.password=your_password
```

---

## 3. Cấu trúc thư mục chính

```text
src/main/java/com/auction
├── client/      JavaFX controllers, client services, socket client, UI helpers
├── server/      socket server, controllers, services, repositories, models
└── shared/      DTOs, protocol classes, exceptions, shared utilities

src/main/resources
├── fxml/        JavaFX FXML screens
├── picture/     UI images/icons
├── css/         stylesheets if any
└── db.properties

src/test/java    Unit tests and smoke tests
docs/            Report, protocol notes, demo links
```

Các package chính:

* `client`: giao diện JavaFX và client-side services.
  * `server`: xử lý nghiệp vụ, transaction, database và socket server.
  * `shared`: DTO, `Request`, `Response`, `ActionType` và exception dùng chung giữa client/server.

---

## 4. Build và chạy test

Chạy lệnh từ thư mục gốc của project.

### Windows

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

### Linux/macOS

```bash
./mvnw clean compile
./mvnw test
```

Nếu đã cài Maven toàn cục:

```bash
mvn clean compile
mvn test
```

---

## 5. Chạy Server và Client

Thứ tự chạy bắt buộc:

1. Khởi động MySQL/TiDB và kiểm tra `db.properties`.
   2. Chạy server.
   3. Chạy một hoặc nhiều JavaFX client.
   4. Đăng nhập bằng tài khoản demo.

---

### 5.1. Chạy Server

Chạy bằng IDE, run server main class:

```text
com.auction.server.Server
```

Hoặc chạy bằng Maven Exec Plugin nếu project đã cấu hình:

#### Windows

```powershell
.\mvnw.cmd exec:java -Dexec.mainClass="com.auction.server.Server"
```

#### Linux/macOS

```bash
./mvnw exec:java -Dexec.mainClass="com.auction.server.Server"
```

Nếu server entry point trong project có tên khác, thay `com.auction.server.Server` bằng class thực tế.

---

### 5.2. Chạy Client

Client entry point:

```text
com.auction.client.Launcher
```

Chạy bằng Maven JavaFX Plugin:

#### Windows

```powershell
.\mvnw.cmd javafx:run
```

#### Linux/macOS

```bash
./mvnw javafx:run
```

Hoặc chạy bằng Maven Exec Plugin nếu project đã cấu hình:

#### Windows

```powershell
.\mvnw.cmd exec:java -Dexec.mainClass="com.auction.client.Launcher"
```

#### Linux/macOS

```bash
./mvnw exec:java -Dexec.mainClass="com.auction.client.Launcher"
```

Chạy bằng IDE:

```text
Run com.auction.client.Launcher
```

---

## 6. Hướng dẫn demo realtime

Để demo realtime với nhiều client:

```text
1. Chạy server.
2. Chạy Client A và đăng nhập user1.
3. Chạy Client B và đăng nhập user2.
4. Mở cùng một phiên đấu giá đang RUNNING trên cả hai client.
5. Client B đặt giá.
6. Client A nhận cập nhật realtime mà không cần refresh.
```

---

## 7. Chức năng đã hoàn thành

### User/Auth

* Đăng ký, đăng nhập, đăng xuất.
  * Phân quyền `USER` và `ADMIN`.
  * Hash mật khẩu bằng BCrypt.
  * Kiểm tra tài khoản bị ban.

### Auction

* Tạo phiên đấu giá.
  * Hiển thị danh sách phiên đấu giá dạng card.
  * Lọc phiên theo trạng thái.
  * Xem chi tiết phiên đấu giá.
  * Hiển thị thời gian bắt đầu, countdown, trạng thái và người thắng.
  * Hủy phiên theo quyền owner/admin.
  * Tự động cập nhật trạng thái bằng server scheduler.

### Bidding

* Đặt giá với validate phía server.
  * Reject bid thấp.
  * Reject seller tự bid.
  * Reject bid khi phiên không còn hợp lệ.
  * Realtime cập nhật giá và lịch sử bid.
  * Gửi `latestBid` trong realtime event.

### Realtime

* Server push event qua socket.
  * Broadcast `AUCTION_UPDATED` tới các client đã đăng nhập.
  * Client tự lọc event theo `auctionId`.
  * Không dùng polling liên tục từ client.

### Wallet/Payment

* Xem số dư.
  * Nạp tiền.
  * Tính số dư khả dụng.
  * Thanh toán phiên đã thắng.
  * Chặn non-winner thanh toán.
  * Chặn thanh toán lặp lại phiên đã `PAID`.

### User personal views

* Xem phiên đã tạo.
  * Xem phiên đã tham gia.
  * Xem phiên đã thắng/thanh toán.
  * Xem lịch sử đặt giá.
  * Mở chi tiết phiên từ lịch sử đặt giá.

### Admin

* Màn hình Admin riêng.
  * Xem danh sách user.
  * Xem danh sách user bị ban.
  * Ban/gỡ ban user.
  * Không cho ban tài khoản `ADMIN`.
  * Xem danh sách phiên đấu giá.
  * Mở chi tiết phiên đấu giá từ AdminScreen.

### Nâng cao/kỹ thuật

* Transaction cho tạo phiên, đặt giá, hủy/đóng phiên và thanh toán.
  * HikariCP connection pool.
  * Anti-sniping: nếu bid hợp lệ trong 2 phút cuối, `endTime = now + 2 phút`.
  * Server-side scheduler: `OPEN → RUNNING → FINISHED`.
  * Unit test cho domain bidding rule, status transition, password hashing và anti-sniping policy.

---

## 8. Hạn chế hiện tại

* Realtime update hiện broadcast tới các client đã đăng nhập, chưa tách room theo từng auction.
  * Request/response matching dựa theo `ActionType`, nên UI tránh gửi nhiều request cùng action đồng thời.
  * Scheduler chạy mỗi 1 giây, nên trạng thái có thể cập nhật trễ khoảng 1 giây.
  * Money hiện còn dùng `double`; phiên bản production nên dùng `BigDecimal` hoặc long cents.
  * Realtime chat và auto-bidding là hướng phát triển sau.

---

## 9. Báo cáo và video demo

* Báo cáo PDF: [Link báo cáo PDF](https://drive.google.com/file/d/1EbnBQGF_Qv9er8CHUDLrT2Syg98vnS0Q/view?usp=sharing)
  * Video demo: [Link video demo](https://drive.google.com/drive/folders/1kPpT0J4_fyH20FVcQmOyPB0lZkVCCkmB?fbclid=IwY2xjawSNO1RleHRuA2FlbQIxMABicmlkETFhTnI5Q1BMZk1ocVh5a2dPc3J0YwZhcHBfaWQQMjIyMDM5MTc4ODIwMDg5MgABHrGehBOYLAW8q4QE1LpDWUkQNIoeGfH2xwcpQhLG7bufg5rYucI1PVMU4BWm_aem_bnpynT-NJFVIYYblaDRi1Q)
