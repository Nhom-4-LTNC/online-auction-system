# Hướng dẫn triển khai Real-time Update

Branch làm việc: `feature/real-time`

Tài liệu này mô tả cách triển khai real-time update cho hệ thống đấu giá online hiện tại. Mục tiêu là làm đủ yêu cầu bài tập, demo ổn định bằng nhiều client, không dùng polling, không dùng WebSocket framework, không sửa lớn protocol hiện có và không over-engineering.

---

## 1. Mục tiêu của feature

Feature real-time cần đảm bảo:

- Khi một client đặt bid thành công, các client khác đang xem cùng phiên đấu giá thấy cập nhật ngay.
- Khi seller close auction thành công, các client đang xem detail/list thấy status cập nhật ngay.
- Khi winner pay auction thành công, các client thấy status cập nhật sang `PAID`.
- Client không cần bấm refresh thủ công.
- Không dùng polling liên tục.
- Không gửi server model qua socket.
- Event gửi qua protocol `Request/Response` hiện tại.

Demo tối thiểu bắt buộc:

```text
Client A mở Auction Detail của auction X.
Client B đặt bid vào auction X.
Client A thấy current price/status/bid history cập nhật ngay.
```

---

## 2. Phương án thiết kế được chọn

Chọn phương án:

```text
Server push qua socket hiện có
+ broadcast toàn bộ client online
+ client tự lọc theo auctionId
+ client-side event dispatcher
```

Không làm subscribe/unsubscribe room theo từng auction trong giai đoạn này.

Lý do:

- Deadline gần, cần ưu tiên demo ổn định.
- Backend socket hiện tại đã có `Server.broadcast(Object data)`.
- `ClientHandler.sendObject(...)` hiện đã `synchronized`, phù hợp cho server-push.
- Số lượng client demo ít, broadcast toàn bộ là chấp nhận được.
- Client tự lọc `auctionId` giúp server đơn giản hơn.
- Sau này nếu cần tối ưu có thể nâng cấp thành auction room.

Cách giải thích với giảng viên:

```text
Server đóng vai trò publisher, các client online đóng vai trò observer.
Khi trạng thái auction thay đổi sau khi transaction commit, server phát event realtime qua socket.
Client nhận event, lọc theo auctionId và cập nhật màn hình tương ứng.
Với quy mô demo nhỏ, broadcast toàn bộ client online là đủ đơn giản và ổn định.
```

---

## 3. Luồng xử lý tổng quát

### 3.1. Khi đặt bid thành công

```text
Client B
  -> Request(PLACE_BID, PlaceBidRequest)

ClientHandler
  -> BidController.handlePlaceBid(...)

BidController
  -> BidService.placeBid(...)

BidService
  -> mở transaction
  -> SELECT auction FOR UPDATE
  -> validate bid
  -> insert bid
  -> update auction/current price/winner nếu có
  -> commit
  -> return kết quả sau commit

BidController
  -> tạo AuctionUpdatedEvent
  -> AuctionEventPublisher.publishAuctionUpdated(event)
  -> return Response.success(PLACE_BID, PlaceBidResponse)

Server
  -> broadcast Response.success(AUCTION_UPDATED, event)

Client A
  -> nhận AUCTION_UPDATED
  -> nếu event.auctionId == currentAuctionId
       -> reload detail hoặc update UI
```

### 3.2. Nguyên tắc quan trọng

Không được broadcast trước khi transaction commit.

Sai:

```text
validate bid
broadcast AUCTION_UPDATED
commit
```

Đúng:

```text
validate bid
update DB
commit
broadcast AUCTION_UPDATED
```

Nếu broadcast trước commit nhưng transaction rollback, client sẽ thấy dữ liệu không tồn tại trong database.

---

## 4. Event protocol

### 4.1. ActionType

Hiện tại `ActionType` đã có:

```java
AUCTION_CREATED,
AUCTION_CLOSED,
AUCTION_UPDATED
```

Trong giai đoạn này, ưu tiên dùng một action chung:

```java
AUCTION_UPDATED
```

Không cần dùng nhiều action riêng như `BID_PLACED_EVENT`, `PAYMENT_COMPLETED_EVENT` ngay.

Lý do:

- Client chỉ cần biết auction nào đã thay đổi.
- Ít nhánh xử lý hơn.
- Dễ mở rộng bằng field `updateType` trong event DTO.

---

## 5. Event DTO đề xuất

Tạo file mới:

```text
src/main/java/com/auction/shared/protocol/event/AuctionUpdatedEvent.java
```

Hoặc nếu project đang gom DTO ở `shared/dto`, có thể đặt:

```text
src/main/java/com/auction/shared/dto/AuctionUpdatedEvent.java
```

Ưu tiên package `shared.protocol.event` vì đây là payload của server-push protocol.

### 5.1. Cấu trúc đề xuất

```java
public class AuctionUpdatedEvent implements Serializable {
    private int auctionId;
    private AuctionUpdateType updateType;
    private AuctionSummaryDTO summary;
    private BidDTO latestBid; // nullable, chỉ có khi BID_PLACED
    private String message;
    private long occurredAtMillis;
}
```

Tạo enum:

```text
src/main/java/com/auction/shared/protocol/event/AuctionUpdateType.java
```

```java
public enum AuctionUpdateType implements Serializable {
    BID_PLACED,
    AUCTION_CLOSED,
    PAYMENT_COMPLETED,
    AUCTION_CANCELED,
    AUCTION_CREATED
}
```

### 5.2. Vì sao không gửi AuctionDetailDTO?

Không nên broadcast `AuctionDetailDTO` mặc định vì detail có thể chứa `ItemDTO`, image data hoặc nhiều dữ liệu nặng.

Lựa chọn cân bằng nhất:

```text
AuctionUpdatedEvent
  -> auctionId
  -> updateType
  -> AuctionSummaryDTO
  -> latestBid nullable
```

AuctionList có thể dùng `summary` để reload/update dòng.
AuctionDetail có thể dùng event để biết cần reload detail/bid history.

---

## 6. Server-side design

### 6.1. File hiện có liên quan

```text
src/main/java/com/auction/server/Server.java
src/main/java/com/auction/server/handler/ClientHandler.java
src/main/java/com/auction/server/controller/BidController.java
src/main/java/com/auction/server/controller/AuctionController.java
src/main/java/com/auction/server/controller/WalletController.java
src/main/java/com/auction/server/service/AuctionObserver.java
src/main/java/com/auction/shared/protocol/ActionType.java
```

### 6.2. Server.broadcast hiện tại

Hiện `Server` đang có:

```java
private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

public static void broadcast(Object data) {
    for (ClientHandler client : clients) {
        client.sendObject(data);
    }
}
```

Cách này dùng được cho demo.

Lưu ý:

- `CopyOnWriteArrayList` phù hợp vì số client ít, add/remove ít.
- Nếu gửi fail, `ClientHandler.sendObject` hiện đã gọi `closeConnections()`.
- `closeConnections()` cần đảm bảo gọi `Server.removeClient(this)`.

### 6.3. ClientHandler.sendObject

Hiện tại:

```java
public synchronized void sendResponse(Response<?> response) {
    sendObject(response);
}

public synchronized void sendObject(Object object) {
    ...
}
```

Giữ nguyên `synchronized`.

Lý do:

- Một client có thể nhận response thường và realtime event gần như cùng lúc.
- Nhiều thread server có thể cùng ghi vào cùng `ObjectOutputStream`.
- `ObjectOutputStream` không nên bị ghi đồng thời từ nhiều thread.

Không được bỏ `synchronized` ở `sendObject`.

---

## 7. AuctionEventPublisher

Thêm file:

```text
src/main/java/com/auction/server/event/AuctionEventPublisher.java
```

Vai trò:

```text
- Nhận AuctionUpdatedEvent
- Gói thành Response.success(ActionType.AUCTION_UPDATED, event)
- Gọi Server.broadcast(...)
```

Pseudo-code:

```java
public class AuctionEventPublisher {
    public void publishAuctionUpdated(AuctionUpdatedEvent event) {
        Response<AuctionUpdatedEvent> response = Response.success(
            ActionType.AUCTION_UPDATED,
            event
        );
        Server.broadcast(response);
    }
}
```

Không nên để service gọi trực tiếp `Server.broadcast(...)`.

Lý do:

- Service nên tập trung business logic và transaction.
- Socket push là tầng infrastructure/server communication.
- Controller hoặc publisher chịu trách nhiệm phát event sau khi service trả về thành công.

---

## 8. Nên broadcast ở đâu?

### 8.1. Quy tắc

Broadcast sau khi service method return success.

Vị trí đề xuất:

```text
Controller -> gọi Service -> Service commit -> Controller tạo event -> AuctionEventPublisher broadcast
```

Ví dụ:

```text
BidController.handlePlaceBid
AuctionController.handleCloseAuction
WalletController.handlePayAuction
```

### 8.2. Không broadcast trong repository

Repository chỉ được:

```text
- chạy SQL
- map ResultSet
```

Không được:

```text
- broadcast socket
- gọi UI
- xử lý protocol
```

### 8.3. Có nên broadcast trong service không?

Có thể làm nếu thiết kế domain event nghiêm túc, nhưng hiện tại chưa cần.

Với deadline gần:

```text
Service xử lý transaction.
Controller/publisher xử lý event sau success.
```

Đây là cách ít phá code nhất.

---

## 9. Client-side design

### 9.1. Vấn đề hiện tại

`Client` hiện có:

```java
private Consumer<Object> onMessageReceived;

public void setOnMessageReceived(Consumer<Object> onMessageReceived) {
    this.onMessageReceived = onMessageReceived;
}
```

Vấn đề:

- Mỗi controller có thể gọi `setOnMessageReceived`.
- Controller sau sẽ ghi đè controller trước.
- AuctionDetail và AuctionList khó cùng nghe event.
- Dễ mất event hoặc update sai màn hình.

### 9.2. Giải pháp: ClientEventDispatcher

Thêm file:

```text
src/main/java/com/auction/client/network/ClientEventDispatcher.java
```

Vai trò:

```text
- Quản lý nhiều listener theo ActionType
- Cho controller đăng ký/hủy đăng ký listener
- Dispatch event realtime tới đúng listener
- Đảm bảo update UI chạy trên JavaFX Application Thread
```

Interface đề xuất:

```java
public class ClientEventDispatcher {
    public void addListener(ActionType action, Consumer<Response<?>> listener) { ... }
    public void removeListener(ActionType action, Consumer<Response<?>> listener) { ... }
    public void dispatch(Response<?> response) { ... }
}
```

Bên trong có thể dùng:

```java
private final Map<ActionType, List<Consumer<Response<?>>>> listeners = new ConcurrentHashMap<>();
```

Danh sách listener có thể là:

```java
CopyOnWriteArrayList
```

vì add/remove ít, dispatch nhiều.

### 9.3. Platform.runLater đặt ở đâu?

Đặt trong `ClientEventDispatcher.dispatch(...)`.

Tức là:

```text
Client.listenForData background thread
  -> nhận Response
  -> nếu là pending response thì đưa vào BlockingQueue
  -> nếu là server-push event thì gọi dispatcher.dispatch(response)

ClientEventDispatcher.dispatch
  -> Platform.runLater(() -> listener.accept(response))
```

Không bắt từng controller tự nhớ gọi `Platform.runLater`.

---

## 10. Sửa Client.listenForData

Hiện tại `Client.listenForData()` đang làm hai việc:

```text
- Nếu response khớp pendingResponses thì offer vào queue
- Nếu onMessageReceived != null thì gọi onMessageReceived
```

Sau refactor, hướng xử lý nên là:

```text
if receivedData instanceof Response<?> response:
    queue = pendingResponses.get(response.getAction())
    if queue != null:
        queue.offer(response)
        return hoặc vẫn cho dispatch tùy action

    if response.getAction() là server-push event:
        eventDispatcher.dispatch(response)
else:
    bỏ qua hoặc log
```

Các server-push event hiện tại:

```java
AUCTION_UPDATED
AUCTION_CREATED // optional
AUCTION_CLOSED  // optional, nếu còn dùng riêng
```

Nhưng mục tiêu giai đoạn này:

```text
Chỉ cần AUCTION_UPDATED.
```

---

## 11. AuctionDetail realtime

### 11.1. Controller cần làm gì?

Ở controller màn detail, ví dụ:

```text
ItemAuctionController
```

hoặc controller đang hiển thị auction detail hiện tại.

Khi mở màn hình:

```text
- biết currentAuctionId
- add listener cho ActionType.AUCTION_UPDATED
```

Khi nhận event:

```text
if event.auctionId != currentAuctionId:
    ignore
else:
    reload detail + bid history
```

### 11.2. Vì sao nên reload thay vì tự patch UI?

Với deadline gần, chọn:

```text
Event tới -> gọi lại getAuctionDetail + getBidHistory -> render lại
```

Lý do:

- Ít bug hơn.
- Không lo duplicate bid trong history.
- Không lo sai thứ tự bid.
- Không lo thiếu field trong event.
- Dữ liệu sau reload chắc chắn khớp DB.

Đây không phải polling vì chỉ reload khi server push event.

### 11.3. Khi rời màn hình có cần remove listener không?

Có.

Nếu controller có lifecycle rõ ràng, cần gọi:

```java
ClientEventDispatcher.removeListener(ActionType.AUCTION_UPDATED, listener);
```

Lý do:

- Tránh memory leak.
- Tránh controller cũ vẫn update UI sau khi đã rời màn hình.
- Tránh nhiều listener bị đăng ký trùng sau mỗi lần mở detail.

Nếu chưa có lifecycle rõ ràng, trước mắt cần tránh đăng ký listener nhiều lần trong cùng một controller.

---

## 12. AuctionList realtime

Với deadline gần, chọn cách đơn giản:

```text
Nhận AUCTION_UPDATED -> gọi load lại list
```

Không cần replace item thủ công ngay.

Lý do:

- Ít bug nhất.
- Không cần xử lý index trong ListView/TableView.
- Không cần đảm bảo equals/hashCode của DTO.
- Dễ demo.

Sau này nếu muốn mượt hơn thì mới nâng cấp:

```text
- tìm item theo auctionId
- replace AuctionSummaryDTO tương ứng
- giữ nguyên filter/sort hiện tại
```

---

## 13. Action nào cần phát AUCTION_UPDATED?

Bắt buộc:

```text
PLACE_BID success
CLOSE_AUCTION success
PAY_AUCTION success
```

Optional:

```text
CREATE_AUCTION success
CANCEL_AUCTION success nếu có
```

Không phát event khi:

```text
- bid bị reject
- seller tự bid bị reject
- bid thấp hơn min bị reject
- pay auction fail
- close auction fail
- request validate fail
```

Chỉ phát event khi dữ liệu thật sự thay đổi trong database và transaction đã commit.

---

## 14. Roadmap triển khai theo patch nhỏ

### Patch 1: Thêm event DTO

Thêm:

```text
src/main/java/com/auction/shared/protocol/event/AuctionUpdatedEvent.java
src/main/java/com/auction/shared/protocol/event/AuctionUpdateType.java
```

Kiểm tra:

```text
- implements Serializable
- có serialVersionUID nếu project đang dùng convention này
- không chứa server model
- không chứa image byte[]
```

---

### Patch 2: Thêm AuctionEventPublisher

Thêm:

```text
src/main/java/com/auction/server/event/AuctionEventPublisher.java
```

Nhiệm vụ:

```text
publishAuctionUpdated(AuctionUpdatedEvent event)
```

Bên trong gọi:

```text
Server.broadcast(Response.success(ActionType.AUCTION_UPDATED, event))
```

---

### Patch 3: Broadcast sau PLACE_BID success

Sửa:

```text
src/main/java/com/auction/server/controller/BidController.java
```

Sau khi `bidService.placeBid(...)` thành công:

```text
- lấy AuctionSummaryDTO mới nhất hoặc map từ kết quả service
- lấy latestBid nếu có
- tạo AuctionUpdatedEvent(updateType = BID_PLACED)
- publish event
- return PLACE_BID response như cũ
```

Không làm thay đổi logic validate bid đang pass manual test.

---

### Patch 4: Thêm ClientEventDispatcher

Thêm:

```text
src/main/java/com/auction/client/network/ClientEventDispatcher.java
```

Sửa:

```text
src/main/java/com/auction/client/network/Client.java
```

Mục tiêu:

```text
- Không dùng setOnMessageReceived làm cơ chế chính cho realtime nữa
- Server-push event được dispatch theo ActionType
- Nhiều controller có thể cùng nghe AUCTION_UPDATED
```

---

### Patch 5: AuctionDetailController realtime

Sửa controller màn detail, ví dụ:

```text
src/main/java/com/auction/client/controller/ItemAuctionController.java
```

Mục tiêu:

```text
- đăng ký listener AUCTION_UPDATED
- lọc event.auctionId == currentAuctionId
- gọi lại load detail + bid history
- render lại UI
- remove listener khi rời màn hình nếu có lifecycle
```

Demo sau patch này:

```text
Client A mở detail.
Client B đặt bid.
Client A thấy detail cập nhật.
```

Đây là milestone quan trọng nhất.

---

### Patch 6: AuctionList realtime

Sửa controller màn list, ví dụ:

```text
src/main/java/com/auction/client/controller/AuctionMenuController.java
```

Mục tiêu:

```text
- đăng ký listener AUCTION_UPDATED
- khi nhận event thì gọi load lại list
```

Không cần update từng dòng trong patch đầu.

---

### Patch 7: Broadcast sau CLOSE_AUCTION success

Sửa:

```text
src/main/java/com/auction/server/controller/AuctionController.java
```

Sau khi close thành công:

```text
publish AUCTION_UPDATED(updateType = AUCTION_CLOSED)
```

Client detail/list sẽ reload theo event.

---

### Patch 8: Broadcast sau PAY_AUCTION success

Sửa:

```text
src/main/java/com/auction/server/controller/WalletController.java
```

Sau khi pay thành công:

```text
publish AUCTION_UPDATED(updateType = PAYMENT_COMPLETED)
```

Client detail/list sẽ reload theo event.

---

### Patch 9: Optional - CREATE_AUCTION realtime

Nếu còn thời gian:

```text
CREATE_AUCTION success -> AUCTION_UPDATED(updateType = AUCTION_CREATED)
```

AuctionList reload để thấy auction mới.

Không bắt buộc cho demo bid realtime.

---

## 15. Checklist thread-safety

Server:

- [ ] Giữ `ClientHandler.sendObject(...)` là `synchronized`.
- [ ] Không ghi trực tiếp vào `ObjectOutputStream` từ nơi khác.
- [ ] `Server.clients` dùng `CopyOnWriteArrayList` là được.
- [ ] Khi client disconnect, gọi `Server.removeClient(this)`.
- [ ] Broadcast fail với một client không được làm hỏng client khác.
- [ ] Không broadcast trước transaction commit.

Client:

- [ ] Không để nhiều controller ghi đè nhau bằng `setOnMessageReceived`.
- [ ] Dùng dispatcher để nhiều listener cùng nghe event.
- [ ] UI update phải chạy trong JavaFX Application Thread.
- [ ] Listener của controller nên được remove khi rời màn hình.
- [ ] Không block JavaFX thread bằng request nặng hoặc sleep.

---

## 16. Checklist không phá kiến trúc

- [ ] Client không gọi `server.service`, `server.repository`, `server.model`.
- [ ] Server không gửi entity/model trực tiếp qua socket.
- [ ] Event payload chỉ dùng DTO/protocol class.
- [ ] Repository không biết gì về socket/event/UI.
- [ ] Service xử lý business logic và transaction.
- [ ] Controller/publisher phát event sau khi service thành công.
- [ ] Protocol vẫn dùng `Response.success(ActionType.AUCTION_UPDATED, event)`.

---

## 17. Kịch bản test thủ công

### Test 1: Bid realtime ở Auction Detail

```text
1. Start server.
2. Start Client A, login user A.
3. Client A mở detail auction X.
4. Start Client B, login user B.
5. Client B đặt bid hợp lệ vào auction X.
6. Client A phải thấy current price/bid history cập nhật mà không bấm refresh.
```

Expected:

```text
- Client B nhận PLACE_BID success.
- Client A nhận AUCTION_UPDATED.
- Client A reload detail/bid history.
- UI không crash JavaFX thread.
```

---

### Test 2: Không update sai auction

```text
1. Client A mở detail auction X.
2. Client B đặt bid vào auction Y.
3. Client A không reload detail auction X.
```

Expected:

```text
Client A nhận event nhưng bỏ qua vì auctionId khác.
```

---

### Test 3: Auction List realtime

```text
1. Client A mở auction list.
2. Client B đặt bid vào auction X.
3. Client A thấy list reload hoặc dòng auction X cập nhật.
```

Expected:

```text
AuctionList không cần bấm refresh thủ công.
```

---

### Test 4: Close auction realtime

```text
1. Client A mở detail auction X.
2. Seller ở Client B close auction X.
3. Client A thấy status đổi CLOSED/FINISHED.
4. Nút bid bị disable hoặc bid tiếp bị server reject.
```

---

### Test 5: Pay auction realtime

```text
1. Auction X đã closed/finished và có winner.
2. Winner ở Client B pay auction.
3. Client A đang xem detail/list thấy status đổi PAID.
```

---

## 18. Những việc chưa làm trong feature này

Không làm trong phase hiện tại:

```text
- WebSocket framework
- polling định kỳ
- subscribe/unsubscribe auction room
- event bus phức tạp
- retry queue cho failed event
- persistent notification
- optimistic UI phức tạp
- broadcast AuctionDetailDTO nặng
```

Có thể nâng cấp sau:

```text
- AuctionRoomRegistry: Map<auctionId, Set<ClientHandler>>
- subscribe khi mở detail, unsubscribe khi rời detail
- chỉ gửi event tới client quan tâm
- update trực tiếp row trong TableView/ListView thay vì reload toàn bộ list
- tách nhiều event action riêng nếu UI cần xử lý khác nhau
```

---

## 19. Quy ước commit cho branch feature/real-time

Nên commit theo patch nhỏ:

```text
git add .
git commit -m "feat(realtime): add auction updated event dto"

git commit -m "feat(realtime): add auction event publisher"

git commit -m "feat(realtime): broadcast auction update after successful bid"

git commit -m "feat(client): add client event dispatcher"

git commit -m "feat(client): update auction detail on realtime event"

git commit -m "feat(client): reload auction list on realtime event"

git commit -m "feat(realtime): broadcast close and payment updates"
```

Không nên gom toàn bộ realtime vào một commit lớn.

---

## 20. Định nghĩa hoàn thành

Feature được coi là hoàn thành khi:

- [ ] `AUCTION_UPDATED` được server push qua socket sau `PLACE_BID` success.
- [ ] Client detail nhận event và cập nhật UI không cần refresh.
- [ ] Client list nhận event và reload/update list.
- [ ] Close auction phát event.
- [ ] Pay auction phát event.
- [ ] Không còn phụ thuộc vào polling.
- [ ] Không gửi server model qua socket.
- [ ] Không có lỗi JavaFX thread khi update UI.
- [ ] Demo được ít nhất 2 client cùng lúc.

---

## 21. Tóm tắt quyết định cuối cùng

Thiết kế real-time của project:

```text
Một action realtime chính: AUCTION_UPDATED
Một event DTO chính: AuctionUpdatedEvent
Server push bằng socket hiện có
Server broadcast toàn bộ client online
Client tự lọc auctionId
Client dùng ClientEventDispatcher để nhiều controller cùng nghe event
AuctionDetail reload detail + bid history khi event liên quan tới auction hiện tại
AuctionList reload list khi có event
```

Đây là phương án đủ sạch, dễ giải thích, ít rủi ro và phù hợp nhất với deadline hiện tại.
