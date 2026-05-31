# TODO - Auction Chat Box (realtime-only)

## Phase 0: Understand & align architecture
- [x] Review realtime pipeline: `Client.addEventListener(ActionType.AUCTION_UPDATED, ...)` -> `Server.broadcastToLoggedIn(...)` -> `ClientEventDispatcher`
- [x] Decide chat to be realtime-only (no DB persistence)

## Phase 1: Protocol & server push
- [x] Add `ActionType.CHAT_MESSAGE`

- [x] Add shared DTO/event model `AuctionChatMessageEvent`

- [x] Implement server service `AuctionChatService` to:
  - [x] validate auctionId exists + auction status open/running
  - [x] validate sender is logged-in
  - [x] broadcast chat message to clients (for now: all logged-in)

## Phase 2: Client-to-server request
- [x] Add client `Request`: `SendAuctionChatRequest`
- [x] Add server controller `AuctionChatController.handleSendChat(...)`
- [x] Update `ClientHandler.dispatch()` switch to handle new request action

## Phase 3: Winner announcement
- [x] When auction closes and winner is known, server creates system message via chat event:
  - [x] `🏆 {winner} đã thắng phiên đấu giá {auctionId}`
  - [x] broadcast with `isSystem=true`



## Phase 4: UI/Client integration
- [x] Update `AuctionDetailView.fxml` to include chat UI:
  - [x] overlay (hidden by default) + toggle button
  - [x] message list (ListView)
  - [x] input TextField
  - [x] send Button

- [x] Update `AuctionDetailController`:
  - [x] subscribe `client.addEventListener(ActionType.CHAT_MESSAGE, ...)`
  - [x] append incoming messages for currentAuctionId
  - [x] send message through `AuctionChatClientService`


## Phase 5: Client service
- [x] Create `AuctionChatClientService` for sending `SendAuctionChatRequest`



## Phase 6: Build & basic manual test
- [ ] Run `mvn compile`
- [ ] Manual test with 2+ clients:
  - [ ] chat works in same auction
  - [ ] winner announcement appears as system message

