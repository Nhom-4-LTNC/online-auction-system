# UI Client Refactor Guide — `feature/ui-refactor`

## 0. Mục tiêu của nhánh

Nhánh `feature/ui-refactor` dùng để refactor UI phía client của hệ thống đấu giá online mà **không rewrite toàn bộ UI**, **không đổi backend lớn**, và **không phá flow demo hiện tại**.

Mục tiêu chính:

1. Controller JavaFX chỉ xử lý UI.
2. Controller không tự tạo `Request`, không tự gửi socket, không tự cast `Response` nếu đã có client-side service tương ứng.
3. Client-side service chịu trách nhiệm tạo request, gửi qua socket, kiểm tra response, cast payload và trả DTO cho controller.
4. Client network chỉ quản lý socket, gửi request, nhận response/event, đóng kết nối.
5. Client không import hoặc gọi trực tiếp bất kỳ lớp nào thuộc `server.*`.
6. Client không gửi `currentUser` để server tin. Server vẫn lấy user hiện tại từ `ClientHandler` sau login.
7. Chuẩn bị nền tảng cho realtime update bằng cách loại bỏ dần `setOnMessageReceived` khỏi controller.
8. Mỗi patch nhỏ, dễ test, dễ rollback.

---

## 1. Bối cảnh kỹ thuật

Project hiện dùng:

- Java Core
- JavaFX + FXML
- Java Socket
- JDBC
- MySQL
- Kiến trúc yêu cầu: Client-Server + MVC
- Giao tiếp client-server bằng object `Request<T>` / `Response<T>` qua socket

Backend core flow đã tương đối ổn và đã pass manual test qua socket:

- Register/Login
- Create auction
- Get auction list/detail
- Place bid hợp lệ
- Reject seller tự bid
- Reject bid thấp
- Close auction
- Reject bid sau close
- Wallet/balance demo đang có hoặc đang hoàn thiện

Protocol hiện tại có:

- `Request<T>`
- `Response<T>` có `action`, `status`, `payload`, `errorMessage`
- Nhiều response wrapper DTO như:
  - `AuthResponse`
  - `GetAllAuctionResponse`
  - `GetAuctionsByTypeResponse`
  - `CreateAuctionResponse`
  - `BalanceResponse`
  - `PayAuctionResponse`

---

## 2. Vấn đề hiện tại trong client

Qua package client hiện tại, các vấn đề chính gồm:

### 2.1 Controller đang làm quá nhiều việc

Một số controller hiện vừa xử lý UI, vừa xử lý protocol, vừa xử lý socket:

- `LoginController`
- `CreateAccountController`
- `AuctionMenuController`
- `AuctionItemMenuController`
- `FinanceMenuController`

Các dấu hiệu cần refactor:

```java
new Request<>(...)
client.sendMessage(...)
client.setOnMessageReceived(...)
response.getPayload()
instanceof AuthResponse
instanceof CreateAuctionResponse
```

Những đoạn này không nên nằm trong controller sau khi đã có client-side service.

---

### 2.2 `setOnMessageReceived` dễ bị controller khác ghi đè

Hiện `Client` có cơ chế kiểu:

```java
client.setOnMessageReceived(...)
```

Vấn đề: nếu nhiều controller cùng gọi `setOnMessageReceived`, controller gọi sau sẽ ghi đè controller trước.

Hậu quả:

- Login có thể không nhận response.
- Auction list có thể không nhận response.
- Create auction có thể nhận nhầm response.
- Realtime event sau này dễ làm vỡ request/response thường.
- Bug khó tái hiện vì phụ thuộc thứ tự mở màn hình.

Nguyên tắc mới:

- Request/response thường dùng `sendRequestAndWait(...)` trong client-side service.
- Realtime event dùng `ClientEventDispatcher`, không để controller tự set handler global.

---

### 2.3 JavaFX utility đang nằm sai package

Hiện controller đang import:

```java
com.auction.shared.util.SceneUtils
com.auction.shared.util.SessionManager
```

Vấn đề:

- `SceneUtils` phụ thuộc JavaFX, không nên nằm trong `shared`.
- `SessionManager` nếu chỉ dùng phía client để hiển thị user/balance/role thì nên nằm trong client.
- `shared` nên chỉ chứa DTO/protocol/enum/common exception/network config.

Cần chuyển dần:

```java
com.auction.shared.util.SceneUtils
```

sang:

```java
com.auction.client.util.SceneUtils
```

và:

```java
com.auction.shared.util.SessionManager
```

sang:

```java
com.auction.client.session.ClientSession
```

---

### 2.4 Controller đang biết quá nhiều về protocol

Ví dụ controller không nên cần biết:

```java
ActionType.LOGIN
ActionType.REGISTER
ActionType.CREATE_AUCTION
ActionType.GET_AUCTIONS_BY_TYPE
Response.Status.SUCCESS
```

Sau refactor, controller chỉ nên gọi service:

```java
UserDTO user = authClientService.login(email, password);
List<AuctionSummaryDTO> auctions = auctionClientService.getAuctionsByType(type);
CreateAuctionResponse result = auctionClientService.createAuction(request);
```

---

### 2.5 Navigation truyền dữ liệu bằng reflection

Nếu đang có logic kiểu:

```java
controller.getClass().getMethod("setAuctionId", int.class).invoke(controller, auctionId);
```

thì nên thay bằng `FXMLLoader` typed controller:

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemAuction.fxml"));
Parent root = loader.load();
ItemAuctionController controller = loader.getController();
controller.initData(auctionId);
stage.setScene(new Scene(root));
```

Reflection chạy được nhưng không an toàn:

- Sai method chỉ lỗi runtime.
- Rename method là vỡ.
- Không có type safety.
- Khó review.

---

## 3. Kiến trúc client đích

Không dùng framework mới. Không cần ViewModel phức tạp lúc này.

Kiến trúc đích:

```text
FXML/View
   ↓
JavaFX Controller
   ↓
Client-side Service
   ↓
Network Client
   ↓
Socket
   ↓
Server
```

Package đề xuất:

```text
com.auction.client
  Launcher.java / MainApp.java

com.auction.client.controller
  LoginController
  RegisterController / CreateAccountController
  AuctionMenuController
  AuctionDetailController / ItemAuctionController
  AuctionItemMenuController / CreateAuctionController
  FinanceMenuController

com.auction.client.service
  AuthClientService
  AuctionClientService
  BidClientService
  WalletClientService

com.auction.client.network
  Client
  ClientEventDispatcher

com.auction.client.session
  ClientSession

com.auction.client.util
  SceneUtils
  AlertUtils
  FormatUtils
```

Package `shared` chỉ nên chứa:

```text
com.auction.shared.dto
com.auction.shared.enums
com.auction.shared.protocol
com.auction.shared.exception
com.auction.shared.network
```

Package `client` không được import:

```java
com.auction.server.*
```

---

## 4. Trách nhiệm từng layer

## 4.1 JavaFX Controller

Controller được phép làm:

- Đọc input từ `TextField`, `Button`, `ListView`, `ComboBox`, `ImageView`.
- Validate input cơ bản.
- Gọi client-side service.
- Render DTO lên UI.
- Show alert.
- Chuyển scene.
- Đăng ký/hủy listener realtime nếu màn hình cần.

Controller không được làm:

- Gọi trực tiếp `server.service`.
- Gọi trực tiếp `server.repository`.
- Dùng `server.model`.
- Truy cập database.
- Tự gửi `currentUser` để server tin.
- Tự tạo `Request` nếu đã có service tương ứng.
- Tự cast `Response` nếu đã có service tương ứng.
- Tự gọi `Client.getInstance().sendMessage(...)`.
- Tự gọi `client.setOnMessageReceived(...)`.

Validation ở controller chỉ nên là validation UI:

- Field rỗng.
- Số không parse được.
- Số <= 0.
- Thiếu item type.
- Thiếu file nếu UI yêu cầu.
- Format đơn giản.

Business validation nằm ở server:

- Seller không được tự bid.
- Bid phải cao hơn giá hiện tại.
- Auction phải còn OPEN.
- User phải đủ available balance.
- User phải là owner/admin mới được close auction.
- User phải là winner mới được pay auction.
- Server mới quyết định quyền nghiệp vụ cuối cùng.

---

## 4.2 Client-side service

Client-side service chịu trách nhiệm protocol.

Nó được phép:

- Tạo request payload.
- Tạo `Request<T>`.
- Gọi `Client.sendRequestAndWait(...)`.
- Kiểm tra `response.isSuccess()` hoặc `response.getStatus()`.
- Cast payload sang response DTO đúng kiểu.
- Throw exception nếu response error.
- Return DTO sạch cho controller.

Nó không nên:

- Update JavaFX UI.
- Show alert.
- Chuyển scene.
- Gọi `Platform.runLater`.
- Truy cập FXML control.

---

## 4.3 Network Client

`Client` chỉ quản lý socket:

- Connect.
- Send request.
- Receive object.
- Map response thường về pending request.
- Dispatch event realtime nếu có.
- Close socket.

Không để controller tự quản lý raw socket.

---

## 4.4 ClientSession

`ClientSession` chỉ dùng để hiển thị phía client:

- current user display name
- role
- balance
- available balance
- ẩn/hiện button

Không dùng `ClientSession` để server tin quyền nghiệp vụ.

Server vẫn phải lấy user hiện tại từ `ClientHandler`.

---

## 5. Thiết kế client-side service cần tạo/sửa

## 5.1 `AuthClientService`

Đề xuất method:

```java
public class AuthClientService {
    public UserDTO login(String email, String password);
    public UserDTO register(String username, String email, String password);
    public void logout(); // nếu protocol có
}
```

Hoặc nếu muốn giữ wrapper:

```java
public AuthResponse login(String email, String password);
public AuthResponse register(String username, String email, String password);
```

Khuyến nghị cho UI: return `UserDTO` để controller gọn hơn.

Controller sau refactor:

```java
UserDTO user = authClientService.login(email, password);
ClientSession.setCurrentUser(user);
SceneUtils.switchScene(...);
```

---

## 5.2 `AuctionClientService`

Đề xuất method:

```java
public class AuctionClientService {
    public List<AuctionSummaryDTO> getAllAuctions();
    public List<AuctionSummaryDTO> getAuctionsByType(ItemType type);
    public AuctionDetailDTO getAuctionDetail(int auctionId);
    public CreateAuctionResponse createAuction(CreateAuctionRequest request);
    public CloseAuctionResponse closeAuction(int auctionId);
}
```

Nếu hiện chưa có `AuctionDetailDTO` hoặc `CloseAuctionResponse`, dùng DTO/protocol hiện có, nhưng không đổi protocol lớn trong patch này.

---

## 5.3 `BidClientService`

Đề xuất method:

```java
public class BidClientService {
    public PlaceBidResponse placeBid(int auctionId, double amount);
    public List<BidDTO> getBidHistoryByAuction(int auctionId);
    public List<BidDTO> getBidHistoryByBidder();
}
```

Nếu backend dùng `BigDecimal`, service có thể nhận `BigDecimal` thay vì `double`.

Controller chỉ parse input và gọi service.

---

## 5.4 `WalletClientService`

Hiện đã có `WalletClientService`. Giữ và chuẩn hóa theo hướng:

```java
public class WalletClientService {
    public BalanceResponse addBalance(double amount);
    public PayAuctionResponse payAuction(int auctionId);
    public BalanceResponse getBalance(); // nếu protocol có
}
```

Không nên expose `setBalance(...)` cho UI demo, trừ khi đó là flow admin/test có action rõ ràng và server check quyền.

---

## 5.5 Exception phía client

Tạo exception chung:

```java
package com.auction.client.service;

public class ClientServiceException extends RuntimeException {
    public ClientServiceException(String message) {
        super(message);
    }
}
```

Service xử lý lỗi:

```java
if (!response.isSuccess()) {
    throw new ClientServiceException(response.getErrorMessage());
}
```

Controller xử lý:

```java
try {
    service.doSomething();
} catch (ClientServiceException e) {
    AlertUtils.showError("Lỗi", e.getMessage());
}
```

---

## 6. Refactor network client

## 6.1 Request/response thường

Các request thường như login, register, get list, create auction, place bid, add balance, pay auction nên đi qua:

```java
Response<?> response = client.sendRequestAndWait(request);
```

hoặc:

```java
Response<?> response = client.sendRequestAndWait(request, timeoutMillis);
```

Không để controller chờ response bằng `setOnMessageReceived`.

---

## 6.2 Rủi ro hiện tại nếu pending response map theo `ActionType`

Nếu `Client` đang map response theo `ActionType`, cần biết đây chỉ là giải pháp tạm đủ demo.

Rủi ro:

- Gửi hai request cùng action gần nhau có thể nhận nhầm response.
- Ví dụ hai request `GET_AUCTIONS_BY_TYPE` chạy song song.

Giải pháp tạm trước deadline:

- Không gửi song song hai request cùng `ActionType`.
- Disable button trong lúc loading.
- Mỗi service call có timeout.
- Không spam refresh.

Giải pháp sau deadline:

- Thêm `requestId` / `correlationId` vào `Request` và `Response`.

---

## 6.3 Realtime event

Để chuẩn bị realtime, tạo `ClientEventDispatcher`.

Ý tưởng tối thiểu:

```java
public class ClientEventDispatcher {
    public void addListener(ActionType eventType, Consumer<Response<?>> listener);
    public void removeListener(ActionType eventType, Consumer<Response<?>> listener);
    public void dispatch(Response<?> event);
}
```

Quy ước action event:

```text
AUCTION_UPDATED
AUCTION_CLOSED
BID_PLACED_EVENT
BALANCE_UPDATED
```

Nếu chưa muốn đổi protocol, tạm quy ước các action trên là server event, không phải response trực tiếp cho request.

Nếu có thời gian sau deadline, nên tách rõ:

```text
MessageType.REQUEST
MessageType.RESPONSE
MessageType.EVENT
```

---

## 6.4 `Platform.runLater`

Quy tắc:

- Client-side service không gọi `Platform.runLater`.
- Controller update UI trên JavaFX Application Thread.
- Event dispatcher có thể đảm bảo listener UI được gọi qua `Platform.runLater`.

Khuyến nghị cho realtime demo:

```java
Platform.runLater(() -> listener.accept(event));
```

đặt trong `ClientEventDispatcher.dispatch(...)` để controller không phải nhớ lặp lại.

---

## 7. Navigation giữa màn hình

## 7.1 `SceneUtils`

`SceneUtils` phải nằm ở:

```java
com.auction.client.util.SceneUtils
```

Không để ở `shared.util`.

---

## 7.2 Truyền `auctionId` sang màn detail

Không dùng reflection.

Dùng `FXMLLoader`:

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemAuction.fxml"));
Parent root = loader.load();

ItemAuctionController controller = loader.getController();
controller.initData(auctionId);

stage.setScene(new Scene(root));
```

Controller detail nên có:

```java
public void initData(int auctionId) {
    this.auctionId = auctionId;
    loadAuctionDetail();
    loadBidHistory();
}
```

---

## 7.3 Kiểm tra FXML

Mỗi controller phải khớp với FXML:

- `fx:controller` đúng package.
- `fx:id` khớp field `@FXML`.
- `onAction` khớp method `@FXML`.
- Resource path đúng.

Không đổi layout lớn trong patch architecture.

---

## 8. DTO phía client

Client nên dùng DTO/protocol/shared enum.

Client được dùng:

```java
AuctionSummaryDTO
AuctionDetailDTO
BidDTO
UserDTO
ItemDTO
ElectronicsDTO
VehicleDTO
ArtDTO
BalanceResponse
PayAuctionResponse
```

Client không được dùng:

```java
server.model.Auction
server.model.Item
server.model.User
server.service.*
server.repository.*
```

Nếu UI cần hiển thị hoặc quyết định ẩn/hiện button, DTO nên có đủ field:

```text
auctionId
sellerId / ownerId
winnerId
itemName
itemType
description
startingPrice
currentPrice
status
startTime
endTime
paid
imageFileName hoặc imageBytes tùy flow hiện tại
```

Lưu ý: ẩn/hiện button phía client chỉ là UX. Server vẫn phải check quyền thật.

---

## 9. Finance / wallet UI

## 9.1 FinanceController nên gọi service nào?

`FinanceMenuController` nên gọi:

```java
WalletClientService
```

Không tự tạo request.

---

## 9.2 Add balance demo

Flow tối thiểu:

```text
User nhập amount
Controller parse amount
Controller check amount > 0
Controller gọi walletClientService.addBalance(amount)
Service gửi request
Server trả BalanceResponse
Controller update label balance/availableBalance
Controller update ClientSession nếu cần
```

Hiển thị nên có:

```text
Balance: ...
Available balance: ...
```

Nếu `availableBalance` chưa có trong DTO, tạm chỉ hiển thị `balance`, không sửa backend lớn trong patch UI.

---

## 9.3 Pay auction button

Với deadline gần, cách đơn giản nhất:

- Đặt nút `Pay Auction` trong màn `AuctionDetail`.
- Chỉ enable/show nếu DTO cho thấy user hiện tại là winner và auction đã FINISHED/CLOSED, chưa paid.
- Khi click thì gọi `walletClientService.payAuction(auctionId)`.
- Sau khi pay thành công, reload detail.

Màn `My Won Auctions` có thể để sau deadline.

---

## 10. Realtime phía client

## 10.1 AuctionDetailController

Khi mở detail:

```text
- load auction detail ban đầu
- register listener AUCTION_UPDATED
- nếu event.auctionId == current auctionId thì update UI
```

Khi rời màn hình:

```text
- remove listener
- set active = false nếu cần
```

Tránh listener cũ update UI đã bị destroy.

---

## 10.2 AuctionMenuController

Khi ở màn auction list:

Có hai cách:

### Cách đơn giản nhất

Nhận event `AUCTION_UPDATED` rồi gọi lại service load list hiện tại.

Ưu điểm:

- Dễ làm.
- Ít bug.
- Không cần tự tìm item trong list.

Nhược điểm:

- Có thể load nhiều hơn cần thiết.

Phù hợp deadline.

### Cách tối ưu hơn

Update đúng item trong `ObservableList` theo `auctionId`.

Để sau nếu còn thời gian.

---

## 10.3 Remove listener

Bắt buộc remove listener khi rời màn hình nếu controller đã đăng ký realtime.

Nếu không:

- Memory leak.
- Controller cũ vẫn nhận event.
- UI cũ bị update sau khi scene đã đổi.
- Có thể phát sinh exception khó debug.

---

## 11. Roadmap refactor theo patch nhỏ

Mỗi patch phải nhỏ, test được, rollback được.

---

## Patch 1 — Dọn utility boundary

Mục tiêu: sửa package sai, ít ảnh hưởng flow.

Làm:

- Tạo `com.auction.client.util.AlertUtils`.
- Tạo/chuyển `com.auction.client.util.SceneUtils`.
- Tạo `com.auction.client.session.ClientSession`.
- Thay dần import:

```java
com.auction.shared.util.SceneUtils
```

bằng:

```java
com.auction.client.util.SceneUtils
```

và:

```java
com.auction.shared.util.SessionManager
```

bằng:

```java
com.auction.client.session.ClientSession
```

Không xóa class cũ ngay nếu còn nơi đang dùng. Có thể để deprecated tạm.

Test sau patch:

- App mở được.
- Login screen mở được.
- Register screen mở được.
- Home navigation hoạt động.
- Back button hoạt động.

---

## Patch 2 — Tạo `AuthClientService`, sửa Login/Register

Mục tiêu: bỏ protocol logic khỏi auth controller.

Tạo:

```java
com.auction.client.service.AuthClientService
```

Sửa:

- `LoginController`
- `CreateAccountController` hoặc `RegisterController`

Controller không còn:

```java
Request
Response
ActionType
Client.getInstance().sendMessage(...)
client.setOnMessageReceived(...)
AuthResponse casting
```

Test sau patch:

- Login đúng.
- Login sai.
- Register đúng.
- Register trùng username/email.
- Sau login chuyển Home được.
- User hiển thị đúng nếu Home có label user.

---

## Patch 3 — Tạo `AuctionClientService` cho auction list

Mục tiêu: sửa `AuctionMenuController`.

Tạo:

```java
com.auction.client.service.AuctionClientService
```

Method tối thiểu:

```java
getAuctionsByType(ItemType type)
getAllAuctions()
```

Sửa `AuctionMenuController`:

- Bỏ `Client` trực tiếp nếu có thể.
- Bỏ `setOnMessageReceived`.
- Bỏ tạo `Request`.
- Bỏ cast `GetAuctionsByTypeResponse` trong controller.
- Gọi `auctionClientService.getAuctionsByType(currentType)`.
- Render `ListView` từ `AuctionSummaryDTO`.

Nếu đang dùng reflection để mở detail, thay bằng `FXMLLoader + getController()`.

Test sau patch:

- Mở auction list.
- Chọn từng item type.
- Refresh list.
- Click một auction chuyển sang detail.

---

## Patch 4 — Làm màn auction detail thật

Mục tiêu: `ItemAuctionController` / `AuctionDetailController` hoạt động rõ ràng.

Hiện `ItemAuctionController.java` có thể đang rỗng, cần implement theo trách nhiệm:

- Nhận `auctionId` qua `initData(int auctionId)`.
- Gọi `auctionClientService.getAuctionDetail(auctionId)`.
- Render thông tin auction/item/currentPrice/status.
- Gọi `bidClientService.getBidHistoryByAuction(auctionId)` nếu có bid history.

Có thể chia nhỏ:

### Patch 4A — Load detail

- Chỉ mở detail và render dữ liệu.

### Patch 4B — Place bid

- Tạo `BidClientService`.
- Controller parse amount.
- Service gửi request place bid.
- Sau bid thành công reload detail/history.

### Patch 4C — Bid history

- Load bid history theo auction.
- Render list/table đơn giản.

### Patch 4D — Close/Pay button

- Close auction nếu owner/admin.
- Pay auction nếu current user là winner và auction đã finished/closed.

Test sau từng patch nhỏ:

- Mở detail từ list.
- Place bid hợp lệ.
- Bid thấp bị reject.
- Seller tự bid bị reject.
- Close auction.
- Bid sau close bị reject.
- Pay auction nếu là winner.

---

## Patch 5 — Refactor create auction

Mục tiêu: sửa `AuctionItemMenuController` hoặc `CreateAuctionController` để không tự xử lý protocol.

Sửa `AuctionClientService` thêm:

```java
createAuction(CreateAuctionRequest request)
```

Controller vẫn có thể:

- Đọc form.
- Validate input cơ bản.
- Chọn ảnh bằng `FileChooser`.
- Build `ItemDTO` hoặc `CreateAuctionRequest`.

Controller không được:

- Tạo `Request<ActionType.CREATE_AUCTION>`.
- Gọi `client.sendMessage`.
- Gọi `setOnMessageReceived`.
- Cast `CreateAuctionResponse`.

Test sau patch:

- Tạo auction Electronics.
- Tạo auction Vehicle.
- Tạo auction Art.
- Bỏ trống field bắt buộc.
- Nhập starting price sai.
- Upload ảnh nếu flow hiện tại hỗ trợ.
- Auction mới xuất hiện trong list.

---

## Patch 6 — Refactor finance/wallet

Mục tiêu: `FinanceMenuController` gọi `WalletClientService` thật.

Làm:

- Dùng `WalletClientService.addBalance(amount)`.
- Parse amount ở controller.
- Check amount > 0 ở controller.
- Update label balance/availableBalance.
- Update `ClientSession` nếu cần.
- Ẩn hoặc bỏ `setBalance` nếu không phải flow admin rõ ràng.

Test sau patch:

- Add balance hợp lệ.
- Add balance âm bị chặn.
- Add balance text không phải số bị chặn.
- Balance hiển thị mới sau response.

---

## Patch 7 — Tạo `ClientEventDispatcher` cho realtime

Mục tiêu: chuẩn bị realtime mà không phá request/response thường.

Làm:

- Tạo `ClientEventDispatcher`.
- `Client.listenForData()` phân biệt response thường và event nếu có thể.
- Event thì gọi dispatcher.
- Controller không gọi `setOnMessageReceived`.

Màn detail:

- Đăng ký listener `AUCTION_UPDATED`.
- Nếu event đúng `auctionId`, reload detail hoặc update current price/status.
- Remove listener khi rời màn hình.

Màn list:

- Đăng ký listener `AUCTION_UPDATED`.
- Khi nhận event, reload list hiện tại.
- Remove listener khi rời màn hình.

Test sau patch:

- Mở cùng auction trên 2 client.
- Client A bid.
- Client B thấy giá update hoặc tự refresh.
- Close auction từ một client.
- Client còn lại thấy status update.

---

## Patch 8 — Dọn import và enforce architecture

Sau khi các flow đã chạy, dọn sạch boundary.

Chạy search toàn project client:

```bash
grep -R "com.auction.server" src/main/java/com/auction/client
```

Không được có kết quả.

Search trong controller:

```bash
grep -R "new Request" src/main/java/com/auction/client/controller
grep -R "Response<" src/main/java/com/auction/client/controller
grep -R "setOnMessageReceived" src/main/java/com/auction/client/controller
grep -R "sendMessage" src/main/java/com/auction/client/controller
grep -R "ActionType" src/main/java/com/auction/client/controller
```

Sau khi service đã tồn tại, controller không nên còn các đoạn này.

Test full demo sau patch:

- Register.
- Login.
- Create auction.
- View auction list.
- View auction detail.
- Place bid.
- Bid history.
- Add balance.
- Close auction.
- Pay auction nếu có.
- Realtime update nếu đã làm.

---

## 12. Những gì không nên làm trong nhánh này

Không làm:

- Không rewrite toàn bộ UI.
- Không đổi layout lớn.
- Không thêm framework mới.
- Không chuyển sang Spring/WebSocket/JPA.
- Không đổi protocol lớn nếu không bắt buộc.
- Không refactor toàn bộ controller trong một commit.
- Không thêm ViewModel phức tạp.
- Không sửa backend nếu patch đang là UI refactor.
- Không làm UI đẹp quá mức trong patch architecture.

Chỉ sửa FXML khi:

- Sai `fx:controller`.
- Sai `fx:id`.
- Sai `onAction`.
- Thiếu control cần thiết cho flow demo.

---

## 13. Checklist review một controller

Một controller đạt yêu cầu nếu:

### Boundary

- [ ] Không import `com.auction.server.*`.
- [ ] Không gọi server service/repository/model.
- [ ] Không truy cập database.
- [ ] Không gửi `currentUser` để server tin.
- [ ] Chỉ dùng DTO/protocol/shared enum nếu cần.

### Network/protocol

- [ ] Không tự tạo `Request` nếu đã có client-side service.
- [ ] Không tự đọc/cast `Response` nếu đã có client-side service.
- [ ] Không gọi `Client.getInstance().sendMessage(...)`.
- [ ] Không gọi `client.setOnMessageReceived(...)`.
- [ ] Không xử lý `Response.error` trực tiếp nếu service đã xử lý.

### UI

- [ ] Đọc input từ UI.
- [ ] Validate input cơ bản.
- [ ] Gọi client-side service.
- [ ] Render DTO lên UI.
- [ ] Show alert qua `AlertUtils`.
- [ ] Chuyển scene qua `SceneUtils`.

### Navigation

- [ ] Không dùng reflection để truyền dữ liệu giữa controller.
- [ ] Dùng `FXMLLoader + getController()` khi cần truyền `auctionId`.
- [ ] FXML khớp `fx:controller`, `fx:id`, `onAction`.

### Session

- [ ] `ClientSession` chỉ dùng để hiển thị user/balance/role.
- [ ] Server vẫn xác thực quyền nghiệp vụ.

### Realtime nếu có

- [ ] Controller đăng ký listener khi vào màn hình.
- [ ] Controller remove listener khi rời màn hình.
- [ ] Listener không update UI nếu controller đã inactive.
- [ ] UI update chạy trên JavaFX Application Thread.

---

## 14. Checklist test sau mỗi patch

Sau mỗi patch, tối thiểu chạy lại flow liên quan.

### Auth patch

- [ ] Login đúng.
- [ ] Login sai.
- [ ] Register đúng.
- [ ] Register fail do trùng user/email.
- [ ] Navigate Home sau login.

### Auction list patch

- [ ] Mở auction list.
- [ ] Filter/list theo type.
- [ ] Refresh list.
- [ ] Click item mở detail.

### Auction detail / bid patch

- [ ] Load detail đúng auction.
- [ ] Place bid hợp lệ.
- [ ] Bid thấp bị reject.
- [ ] Seller tự bid bị reject.
- [ ] Bid history cập nhật.

### Create auction patch

- [ ] Tạo auction hợp lệ.
- [ ] Missing field bị chặn.
- [ ] Giá sai bị chặn.
- [ ] Auction mới xuất hiện trong list.

### Wallet patch

- [ ] Add balance hợp lệ.
- [ ] Amount âm bị chặn.
- [ ] Amount không phải số bị chặn.
- [ ] Balance hiển thị đúng sau response.

### Close/pay patch

- [ ] Owner/admin close được auction.
- [ ] Non-owner bị server reject.
- [ ] Winner pay được.
- [ ] Non-winner bị server reject.

### Realtime patch

- [ ] Hai client cùng mở một auction.
- [ ] Client A bid, client B thấy update.
- [ ] Client A close, client B thấy status update.
- [ ] Rời màn hình không còn listener cũ update UI.

---

## 15. Quy tắc commit/merge

Commit nên nhỏ và rõ mục tiêu.

Ví dụ commit tốt:

```text
refactor(client): add AuthClientService
refactor(ui): move SceneUtils to client.util
refactor(ui): remove socket handling from LoginController
refactor(ui): add AuctionClientService for auction list
refactor(ui): replace reflection navigation with FXMLLoader controller init
```

Commit không tốt:

```text
update ui
fix stuff
refactor all
big changes
```

Mỗi pull request hoặc merge nên có:

- Mô tả patch sửa gì.
- Flow đã test.
- Screenshot nếu sửa UI nhìn thấy được.
- Ghi rõ có đổi protocol/backend hay không.

Không merge nếu:

- App không chạy.
- Login flow vỡ.
- Controller mới import `server.*`.
- Controller mới dùng `setOnMessageReceived`.
- Patch architecture nhưng đổi layout lớn không cần thiết.

---

## 16. Thứ tự ưu tiên nếu gần deadline

Nếu không đủ thời gian làm hết, ưu tiên theo thứ tự:

1. AuthClientService cho Login/Register.
2. AuctionClientService cho Auction list.
3. Auction detail load được bằng DTO.
4. BidClientService cho Place bid.
5. Create auction qua AuctionClientService.
6. Wallet add balance qua WalletClientService.
7. Close/pay button nếu demo cần.
8. ClientEventDispatcher realtime tối thiểu.
9. Dọn import cuối cùng.

Nếu rất sát deadline, bỏ qua phần realtime phức tạp. Chỉ cần đảm bảo flow demo chính chạy ổn và controller không phá kiến trúc nghiêm trọng.

---

## 17. Definition of Done cho nhánh `feature/ui-refactor`

Nhánh được coi là đạt nếu:

- [ ] Login/Register chạy qua `AuthClientService`.
- [ ] Auction list chạy qua `AuctionClientService`.
- [ ] Create auction chạy qua `AuctionClientService`.
- [ ] Place bid chạy qua `BidClientService`.
- [ ] Add balance/pay auction chạy qua `WalletClientService` nếu demo có.
- [ ] Controller không còn gọi trực tiếp socket cho các flow đã có service.
- [ ] Controller không còn `setOnMessageReceived` cho request/response thường.
- [ ] Client không import `com.auction.server.*`.
- [ ] `SceneUtils` nằm trong `client.util`.
- [ ] `ClientSession` nằm trong `client.session`.
- [ ] Navigation detail không dùng reflection.
- [ ] Full manual demo pass.

---

## 18. Ghi chú cuối

Ưu tiên của nhánh này là **demo ổn định và boundary đúng**, không phải code hoàn hảo tuyệt đối.

Nguyên tắc quan trọng nhất:

```text
Controller mỏng lại.
Protocol logic đi vào ClientService.
Socket logic nằm ở Client.
Realtime không dùng setOnMessageReceived trong từng controller.
Không phá flow đang chạy.
```
