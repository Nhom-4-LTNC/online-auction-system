# Codebase Final Review

Date: 2026-06-02

## 1. Build/Test Result

| Check | Command | Result | Note |
|---|---|---|---|
| Compile baseline | `.\mvnw.cmd -q -DskipTests compile` | PASS | Only JDK/Maven warning about native access/Unsafe. |
| Test baseline | `mvn test` | initially blocked, then FAIL | Sandbox blocked Maven plugin download. With network permission, tests ran and 1 DB-fixture-dependent test failed. |
| Compile final | `.\mvnw.cmd -q -DskipTests compile` | PASS | After cleanup/fixes. |
| Test final | `mvn test` | PASS | 11 tests run, 0 failures, 0 errors, 1 skipped. Test command needs Maven plugin resolution outside the restricted sandbox. |

Skipped test:

| Test | Reason |
|---|---|
| `BidServiceTest.placeBid_whenInsufficientFunds_shouldThrowInsufficientFundsException` | Current DB has no `OPEN`/`RUNNING` auction fixture, so the smoke test is skipped with JUnit assumption. |

## 2. Current Feature Status

| Feature | Status | Evidence | Note |
|---|---|---|---|
| Auth/Register/Login | DONE | `AuthController`, `AuthService`, `LoginController`, `CreateAccountController` | Client calls are async in login/register controllers. |
| Logout | DONE | `ClientHandler.LOGOUT`, `AuthClientService.logout`, `AuctionMenuController.handleBack`, `AdminMenuController.handleLogout` | Client now sends server-side logout before clearing local session. |
| BCrypt password hashing | DONE | `PasswordHasher`, `AuthService` | Server-side auth owns password verification. |
| Role USER/ADMIN | DONE | `Role`, `ClientSession`, `UserService.requireAdmin`, `LoginController.navigateAfterLogin` | ADMIN routes to AdminScreen, USER routes to AuctionMenu. |
| Admin ban/unban user | DONE | `AdminController`, `UserService.applyBan/removeBan`, `AdminUsersViewController` | Server blocks banning ADMIN accounts; client hides BAN for ADMIN/current admin. |
| Auction create/list/detail | DONE | `AuctionController`, `AuctionService`, `AuctionMenuController`, `AuctionDetailController` | Summary/detail DTOs are used, not server models. |
| Auction update/cancel | DONE | `ActionType.UPDATE_AUCTION_ITEM`, `ActionType.CANCEL_AUCTION`, `UpdateAuctionRequest`, `AuctionService.updateAuctionItem/cancelAuction` | Owner update/cancel is limited to OPEN; admin can cancel through detail. |
| Auction status lifecycle | DONE/RISKY | `Auction.refreshStatus`, `AuctionStatusScheduler`, `AuctionRepository.finalizeExpiredAuctionsForRead` | Scheduler works, but read-time finalization can race with realtime broadcast. |
| Place bid + validation | DONE | `BidService.placeBid`, `Auction.placeBid`, `AuctionValidator` | Transaction uses row lock and wallet available balance check. |
| Anti-sniping | DONE | `BidService.applyAntiSnipingExtension` | Default rule extends end time when a valid bid lands in the last 2 minutes. |
| Realtime `AUCTION_UPDATED` | DONE/RISKY | `AuctionEventPublisher`, `AuctionUpdatedEvent`, `ClientEventDispatcher`, AuctionMenu/AuctionDetail listeners | Listener cleanup exists. Read-time status finalization remains a realtime consistency risk. |
| latestBid realtime payload | DONE/PARTIAL | `AuctionUpdatedEvent.getLatestBid`, `BidController.handlePlaceBid`, `AuctionDetailController.addLatestBidToHistory` | Field is still named `lastestBid`; alias getter avoids client breakage. |
| Bid history responses | DONE | `BidRepository.getBidDTOsByAuctionId/getBidDTOsByBidderId` | DTO queries join `users`, avoiding N+1 for bidder username. |
| Wallet/deposit/available balance | DONE | `WalletService`, `WalletController`, `AuctionMenuController.showDepositDialog` | Deposit runs in JavaFX `Task`. |
| Pay auction | DONE | `PaymentService.payAuction`, `WalletController.handlePayAuction`, AuctionMenu card pay action | Server validates winner/status and prevents pay twice. |
| AuctionDetail countdown/winner/startTime | DONE | `AuctionDetailController`, `AuctionDetailView.fxml` | Timeline is stopped in `cleanup()`. |
| AuctionMenu card winner | DONE | `AuctionMenuController.createAuctionCard`, `AuctionSummaryDTO.winnerUsername` | Cards use summary DTO only. |
| My created/participated/won auctions | DONE | `AuctionController.handleGetMy*`, `AuctionClientService.getMy*`, `AuctionMenuController` | Uses summary list endpoints. |
| User bid history UI | DONE | `AuctionMenuController.handleShowMyBids`, `BidClientService.getMyBids` | Modal loads bid history in background and can open auction detail by ID. |
| AdminScreen | DONE | `AdminScreen.fxml`, `AdminMenuController` | Table is cleaned, status formatted in Vietnamese, detail action exists. |
| HomeMenu legacy | DONE | Search: no `HomeMenu.fxml`, `HomeScreen.fxml`, `HomeMenuController` references | Only method name `navigateToHome` remains but routes to `AuctionMenu.fxml`. |

## 3. Cleanup Performed

| File | Change | Reason | Risk |
|---|---|---|---|
| `pom.xml` | Removed `maven-javadoc-plugin` attach-javadocs config | It was not needed for demo/runtime and blocked `mvn test` when plugin was absent from local cache. | Low |
| `src/main/java/com/auction/server/repository/AuctionRepository.java` | Removed unused `getAuctionsByType(Connection, String)` | No references; query used a non-existent `auctions.item_type` path and `pstmt.toString()` as SQL. | Low |
| `src/main/java/com/auction/client/controller/AuctionDetailController.java` | Formatted status label in Vietnamese | Removed user-facing enum English in detail screen. | Low |
| `src/main/resources/fxml/AdminScreen.fxml` | Removed misleading system-setting button; added `fx:id` for canceled-auction button | The removed button was labeled user-management but called `refresh`. | Low |
| `src/main/java/com/auction/client/controller/AdminMenuController.java` | Moved auction table loading/filtering to JavaFX `Task` and disabled table/buttons while loading | Prevents socket request from blocking JavaFX Application Thread. | Low |
| `src/main/java/com/auction/client/service/AuthClientService.java` | Added `logout()` wrapper for existing `LOGOUT` action | Lets client clear server-side session. | Low |
| `src/main/java/com/auction/client/controller/AuctionMenuController.java` | Calls server-side logout asynchronously before local logout | Prevents stale server-side logged-in socket after user logout. | Low |
| `src/main/java/com/auction/client/controller/AdminMenuController.java` | Calls server-side logout asynchronously before local logout | Same as above for admin flow. | Low |
| `src/test/java/com/auction/server/service/BidServiceTest.java` | Converted missing FINISHED/OPEN-RUNNING DB fixture checks to JUnit assumptions | Keeps smoke tests from failing before the service path is exercised. | Low |

## 4. Remaining Risks

| Risk | Severity | Area | Evidence | Suggested fix | Should fix before demo? |
|---|---|---|---|---|---|
| Read methods can finalize expired auction status without broadcasting realtime event. | P1 | Backend/Realtime | `AuctionService.getAllAuctions/getAuctions*` call `AuctionRepository.finalizeExpiredAuctionsForRead`; scheduler only broadcasts changes it performs. | Centralize status transition in scheduler/service and return/publish changed summaries, or stop mutating status in read paths. | Yes, if auto-finish realtime is demo-critical. |
| Client request correlation is keyed only by `ActionType`. | P2 | Client/Protocol | `Client.pendingResponses: Map<ActionType, BlockingQueue<Response<?>>>` | Add requestId/correlationId after demo; until then keep UI buttons disabled during same-action requests. | No |
| Tests depend on live MySQL DB state. | P2 | Test/DB | Repository/service tests read current DB; 1 bid test skipped due missing active auction. | Add isolated test DB fixtures or transactional setup/teardown. | No, but seed demo DB deliberately. |
| `AuctionUpdatedEvent` still has typo field `lastestBid`. | P2 | Shared DTO/Realtime | Field and getter `getLastestBid`; alias `getLatestBid` exists. | Rename only in a coordinated protocol cleanup where client/server are rebuilt together. | No |
| AdminScreen does not subscribe to realtime updates directly. | P3 | Admin/UI/Realtime | `AdminMenuController` reloads on open/filter/detail close, no `AUCTION_UPDATED` listener. | Add same listener pattern as AuctionMenu if admin table must live-update during demo. | No |
| Legacy `AuctionService.getAllAuctionSummaries()` does not finalize statuses and catches errors as empty list. | P3 | Backend/Test | Method is used by tests; main controller uses `getAllAuctions()`. | Deprecate/remove after tests are migrated to proper fixtures. | No |
| Server console logs are noisy during broadcast/scheduler events. | P3 | Backend/Logging | `Server.broadcastToLoggedIn`, `AuctionEventPublisher`, `AuctionStatusScheduler` print per event/change. | Replace with a small logger and reduce INFO volume after demo. | No |
| Double is used for money values. | P3 | Backend/DB | DTO/service/repository use `double` for prices/balances. | Convert to `BigDecimal` only in a dedicated post-demo patch. | No |

## 5. Do Not Touch Before Demo

- Do not refactor the whole Request/Response protocol to requestId unless there is time for full client/server testing.
- Do not rename `AuctionUpdatedEvent.lastestBid` before demo; use `getLatestBid()` alias.
- Do not convert all money values from `double` to `BigDecimal` in this patch window.
- Do not redesign `AuctionMenuController` or the JavaFX layout system.
- Do not rewrite database schema or seed strategy right before demo.
- Do not add realtime bid price chart, auto-bidding, subscribe rooms, or large protocol features.
- Do not change anti-sniping constants unless the demo script explicitly depends on another window.
- Do not remove legacy repository/model methods that are still used by current tests without migrating those tests first.

## 6. Final Demo Checklist

| Item | Status | Note |
|---|---|---|
| Login USER | Ready | Should route to AuctionMenu. |
| Login ADMIN | Ready | Should route to AdminScreen. |
| Logout USER/ADMIN | Ready | Client now sends server-side `LOGOUT` asynchronously and clears `ClientSession`. |
| Create auction modal | Ready | Verify item image/type-specific fields manually. |
| Auction list/card/filter | Ready | Card uses summary DTO; no detail call per card. |
| Auction detail/countdown | Ready | Verify OPEN and RUNNING countdown manually. |
| Bid valid | Ready | Requires RUNNING auction and funded bidder. |
| Reject low bid | Ready | Server validation in `Auction.placeBid`. |
| Reject self bid | Ready | Server validation in `Auction.placeBid`. |
| Anti-sniping | Ready | Demo with auction ending within 2 minutes; end time should extend. |
| Auto status scheduler | Risk | Scheduler runs every 1 second; avoid manual refresh exactly at expiry when demonstrating realtime. |
| Realtime 2 clients | Risk | Bid/update/cancel/pay broadcasts are ready; auto-finish broadcast has read-finalization race risk. |
| Close/cancel auction | Ready | UI keeps cancel; backend close still exists but is not primary UI flow. |
| Payment | Ready | Winner-only, FINISHED-only, prevents PAID repeat. |
| Deposit | Ready | Runs async and updates displayed balance. |
| My bids history | Ready | Modal loads async; "Xem phiên" opens detail by auctionId. |
| Admin users ban/unban | Ready | ADMIN accounts cannot be banned. |
| Admin auction detail | Ready | Table has "Xem chi tiết"; modal refreshes table on close. |
