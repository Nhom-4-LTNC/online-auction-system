# PROTOCOL_CONTRACT.md

# Hợp đồng giao tiếp Client-Server

Tài liệu này quy định cách client và server giao tiếp trong hệ thống đấu giá online. Mọi thành viên khi viết UI, controller, service hoặc thêm tính năng mới cần tuân thủ tài liệu này để không phá kiến trúc Client-Server.

---

## 1. Quy tắc quan trọng nhất

**Client không được gọi trực tiếp service, repository hoặc model nội bộ của server.**

Không làm:

```java
AuctionService.getInstance().placeBid(...);      // SAI nếu đây là server service
UserService.getInstance().updateUserBalance(...); // SAI nếu đây là server service
```

Client chỉ được giao tiếp với server thông qua:

```text
Request -> Socket -> Server -> Response
```

Luồng đúng:

```text
JavaFX Controller
  -> Client-side Service
  -> Client Network
  -> Request qua socket
  -> Server ClientHandler
  -> Server Controller
  -> Server Service
  -> Repository
  -> Database
  -> Response trả về client
```

---

## 2. Package nào được dùng ở client?

Client được dùng:

```java
com.auction.client.*
com.auction.shared.dto.*
com.auction.shared.protocol.*
com.auction.shared.enums.*
com.auction.shared.exception.* // nếu exception dùng chung thật sự
```

Client không được import:

```java
com.auction.server.service.*
com.auction.server.repository.*
com.auction.server.model.*
com.auction.server.database.*
```

Nếu thấy controller phía client import `server.*`, cần sửa trước khi merge.

---

## 3. Request là gì?

Client gửi request theo dạng:

```java
Request<T> request = new Request<>(ActionType.SOME_ACTION, payload);
```

Trong đó:

```text
ActionType = hành động muốn server xử lý
payload = dữ liệu gửi kèm, thường là một request DTO
```

Ví dụ đặt giá:

```java
PlaceBidRequest payload = new PlaceBidRequest(auctionId, amount);

Request<PlaceBidRequest> request =
        new Request<>(ActionType.PLACE_BID, payload);
```

---

## 4. Response là gì?

Server trả về:

```java
Response<T>
```

Response có các thông tin chính:

```text
action       = action tương ứng
status       = SUCCESS hoặc ERROR
payload      = dữ liệu trả về nếu thành công
errorMessage = thông báo lỗi nếu thất bại
```

Client phải luôn kiểm tra response trước khi dùng payload:

```java
if (response.isSuccess()) {
    T payload = response.getPayload();
    // render UI
} else {
    String error = response.getErrorMessage();
    // hiển thị lỗi
}
```

Không được giả định server luôn trả thành công.

---

## 5. Client-side service là gì?

Controller JavaFX không nên tự xử lý socket quá nhiều. Nên tạo service phía client để đóng gói request/response.

Ví dụ:

```text
AuctionMenuController
  -> AuctionClientService.getAuctionListByType(...)
  -> gửi Request
```

```text
ItemAuctionController
  -> BidClientService.placeBid(...)
  -> gửi Request
```

Tên gợi ý:

```text
AuthClientService
AuctionClientService
BidClientService
UserClientService
FinanceClientService
```

Lưu ý: đây là service phía client, không phải `server.service`.

---

## 6. Server lấy user hiện tại từ đâu?

Client không được gửi nguyên `User` object lên server để server tin.

Không làm:

```java
placeBid(auctionId, currentUser, amount); // SAI
```

Đúng:

```java
placeBid(auctionId, amount); // client chỉ gửi auctionId và amount
```

Server sẽ lấy user hiện tại từ session của `ClientHandler`:

```java
UserDTO currentUser = clientHandler.getCurrentUser();
```

Lý do: nếu server tin `User` object từ client, client có thể giả mạo user khác.

---

## 7. Client chỉ dùng DTO, không dùng server model

Client không được dùng trực tiếp:

```java
Auction
Item
User
Bid
```

nếu các class đó nằm trong `com.auction.server.model`.

Client nên dùng DTO:

```java
AuctionSummaryDTO
AuctionDetailDTO
BidDTO
UserDTO
CreateAuctionRequest
PlaceBidRequest
```

Server chịu trách nhiệm map:

```text
Model -> DTO
DTO -> Model/use case input
```

---

## 8. Ví dụ đúng: lấy danh sách auction theo loại item

### Client controller

```java
List<AuctionSummaryDTO> auctions =
        auctionClientService.getAuctionListByType(ItemType.ELECTRONICS);
```

### Client-side service

```java
public List<AuctionSummaryDTO> getAuctionListByType(ItemType type) {
    Request<ItemType> request =
            new Request<>(ActionType.GET_AUCTIONS_BY_TYPE, type);

    Response<List<AuctionSummaryDTO>> response = client.sendRequest(request);

    if (!response.isSuccess()) {
        throw new RuntimeException(response.getErrorMessage());
    }

    return response.getPayload();
}
```

### Server

```java
case GET_AUCTIONS_BY_TYPE -> auctionController.handleGetAuctionsByType(client, request);
```

Server controller gọi server service:

```java
List<AuctionSummaryDTO> auctions =
        auctionService.getAuctionListByType(type);

return Response.success(ActionType.GET_AUCTIONS_BY_TYPE, auctions);
```

---

## 9. Ví dụ đúng: đặt giá

### Client controller

```java
bidClientService.placeBid(auctionId, amount);
```

### Client-side service

```java
public PlaceBidResponse placeBid(int auctionId, double amount) {
    PlaceBidRequest payload = new PlaceBidRequest(auctionId, amount);

    Request<PlaceBidRequest> request =
            new Request<>(ActionType.PLACE_BID, payload);

    Response<PlaceBidResponse> response = client.sendRequest(request);

    if (!response.isSuccess()) {
        throw new RuntimeException(response.getErrorMessage());
    }

    return response.getPayload();
}
```

### Server

Server không lấy user từ payload. Server lấy từ session:

```java
UserDTO currentUser = clientHandler.getCurrentUser();

bidService.placeBid(
        currentUser.getId(),
        request.getAuctionId(),
        request.getAmount()
);
```

---

## 10. Ví dụ đúng: nạp tiền

Chức năng nạp tiền nên là cộng thêm balance, không phải user tự set balance tuyệt đối.

### ActionType

```java
ADD_BALANCE
```

### Request DTO

```java
public class AddBalanceRequest implements Serializable {
    private final double amount;

    public AddBalanceRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}
```

### Response DTO

```java
public class BalanceResponse implements Serializable {
    private final double newBalance;

    public BalanceResponse(double newBalance) {
        this.newBalance = newBalance;
    }

    public double getNewBalance() {
        return newBalance;
    }
}
```

### Client

```java
BalanceResponse response = financeClientService.addBalance(amount);
SessionManager.getInstance().getCurrentUser().setBalance(response.getNewBalance());
```

### Server

Server lấy current user từ session:

```java
UserDTO currentUser = clientHandler.getCurrentUser();

double newBalance = userService.addBalance(currentUser.getId(), request.getAmount());

return Response.success(
        ActionType.ADD_BALANCE,
        new BalanceResponse(newBalance)
);
```

Không cho user thường tự gọi `SET_BALANCE`. Nếu cần set balance tuyệt đối, đó là admin action và server phải check role ADMIN.

---

## 11. Realtime / subscribe auction

Không tự tạo message ngoài protocol như:

```java
new SubscribeAuctionRequest(...)
new AuctionResponse(...)
ActionType.NOTIFY_NEW_BID
```

trừ khi các class/action này đã được thêm chính thức vào protocol và server đã xử lý.

Nếu chưa có realtime chính thức, dùng cách đơn giản:

```text
placeBid thành công -> client gọi lại getAuctionDetail() để refresh UI
```

Nếu muốn realtime chính thức, cần thống nhất thêm:

```text
ActionType.SUBSCRIBE_AUCTION
ActionType.UNSUBSCRIBE_AUCTION
ActionType.AUCTION_UPDATED
```

Payload nên là DTO, ví dụ:

```text
AuctionSubscriptionRequest
AuctionDetailDTO
BidDTO
```

Không gửi server model `Auction` trực tiếp cho client.

---

## 12. Thêm tính năng mới theo quy trình nào?

Khi thêm một tính năng mới, làm theo thứ tự:

```text
1. Thêm ActionType
2. Tạo request DTO nếu cần
3. Tạo response DTO nếu cần
4. Tạo method trong client-side service
5. Client controller gọi client-side service
6. Server ClientHandler/dispatcher route action
7. Server controller parse request
8. Server service xử lý nghiệp vụ
9. Repository truy cập DB nếu cần
10. Server trả Response
11. Client kiểm tra response.isSuccess()
```

Không được bỏ qua socket/protocol để gọi thẳng server service.

---

## 13. Những điều không được làm

Không gọi server service từ client:

```java
AuctionService.getInstance()
UserService.getInstance()
BidService.getInstance()
```

Không import server model trong client:

```java
import com.auction.server.model.User;
import com.auction.server.model.auction.Auction;
```

Không tự tạo message ngoài protocol:

```java
new SubscribeAuctionRequest(...) // nếu chưa có trong protocol chính thức
new AuctionResponse(...)         // nếu không thuộc protocol chính thức
```

Không truyền `currentUser` từ client lên server để server tin:

```java
placeBid(auctionId, currentUser, amount)
```

Không để client tự cập nhật DB hoặc gọi repository.

---

## 14. Quy tắc ngắn gọn cần nhớ

```text
Client chỉ làm UI và gửi Request.
Server mới xử lý nghiệp vụ.
Repository chỉ server được gọi.
Database chỉ server được truy cập.
Client và server giao tiếp bằng DTO + Request/Response.
```

Nếu mọi người tuân thủ các quy tắc này, code sẽ giữ đúng kiến trúc Client-Server + MVC.
