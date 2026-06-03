# Online Auction System - Comprehensive Documentation

**Version:** 1.0-SNAPSHOT  
**Date:** June 3, 2026  
**Language:** Java 21, JavaFX 21, MySQL 8.0

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Installation & Setup](#installation--setup)
4. [Project Structure](#project-structure)
5. [Feature Documentation](#feature-documentation)
6. [Database Schema](#database-schema)
7. [API & Protocol Documentation](#api--protocol-documentation)
8. [Client-Side Architecture](#client-side-architecture)
9. [Server-Side Architecture](#server-side-architecture)
10. [User Guides](#user-guides)
11. [Development Guidelines](#development-guidelines)
12. [Testing](#testing)
13. [Known Issues & Risks](#known-issues--risks)
14. [Troubleshooting](#troubleshooting)

---

## Project Overview

### Purpose

The **Online Auction System** is a full-stack desktop auction platform that allows users to:
- Create and manage auctions for various item types (Art, Electronics, Vehicles, General Items)
- Place bids on live auctions with real-time updates
- Manage digital wallets with deposit and payment functionality
- Experience anti-sniping mechanisms
- Browse auction history and bid history

### Key Features

- **User Authentication:** Secure login with BCrypt password hashing
- **Role-Based Access:** USER and ADMIN roles with different permissions
- **Auction Lifecycle:** OPEN → RUNNING → FINISHED → PAID/CANCELED states
- **Real-Time Bidding:** Live bid updates to all connected clients
- **Wallet System:** Deposit funds and validate available balance before bidding
- **Anti-Sniping:** Auto-extend auction end time when bids arrive in final 2 minutes
- **User Management:** Admin can ban/unban users
- **Item Management:** Create auctions with type-specific attributes (Artist, Brand, VIN, etc.)
- **Bid History Visualization:** Line chart showing bid progression over time

### Technology Stack

| Layer | Technology |
|-------|-----------|
| **UI Framework** | JavaFX 21 with FXML layouts |
| **Backend** | Java 21, Socket-based TCP communication |
| **Database** | MySQL 8.0 with HikariCP connection pooling |
| **Security** | BCrypt for password hashing |
| **Build Tool** | Maven with `-Dskip-Tests` option |
| **Testing** | JUnit 5 |

---

## Architecture

### High-Level Architecture

```
┌─────────────────────┐
│   JavaFX Client     │
│  (Windows/Desktop)  │
└──────────┬──────────┘
           │ TCP Socket
           │ Request/Response Protocol
           │
┌──────────▼──────────┐
│  Server (TCP Port)  │
│  - ClientHandler    │
│  - Controllers      │
│  - Services         │
│  - Schedulers       │
└──────────┬──────────┘
           │ JDBC
           │
┌──────────▼──────────┐
│   MySQL Database    │
│  - Users            │
│  - Auctions         │
│  - Items            │
│  - Bids             │
└─────────────────────┘
```

### Client-Server Communication Flow

```
JavaFX Controller
     ↓
Client Service (AuctionClientService, BidClientService, etc.)
     ↓
Request Object (ActionType + Payload)
     ↓
Client Network (TCP Socket)
     ↓
Server ClientHandler (dispatches by ActionType)
     ↓
Server Controller (parses request)
     ↓
Server Service (business logic)
     ↓
Repository (database access)
     ↓
MySQL Database
     ↓
Response Object (Status + Payload/ErrorMessage)
     ↓
JavaFX Controller (renders UI)
```

### Real-Time Event Flow

```
Bid Placed
    ↓
BidService.placeBid()
    ↓
AuctionEventPublisher.publishBidPlaced()
    ↓
ClientEventDispatcher.broadcastToLoggedIn()
    ↓
AUCTION_UPDATED event to all connected clients
    ↓
AuctionDetailController listener updates chart & table
```

---

## Installation & Setup

### Prerequisites

- **JDK 21** or higher
- **Maven 3.8+** or use the bundled Maven Wrapper
- **MySQL 8.0+** running and accessible
- **Git** for cloning the repository

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/online-auction-system.git
cd online-auction-system
```

### Step 2: Configure Database

Create `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/auction_db
db.username=root
db.password=your_password
db.max-pool-size=10
db.min-idle=5
```

### Step 3: Create Database Schema

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

This creates:
- `users` table with default ADMIN user (username: `admin`, password: `admin`)
- `items` table
- `auctions` table
- `bids` table

### Step 4: Build Project

```bash
# Using Maven Wrapper (Windows)
.\mvnw.cmd clean compile

# Linux/Mac
./mvnw clean compile
```

### Step 5: Start Server

From your IDE (IntelliJ, VS Code, etc.):
1. Open `src/main/java/com/auction/server/Server.java`
2. Run the main method

Or from terminal:
```bash
java -cp target/classes com.auction.server.Server
```

The server will print: `Server is listening on port 5000`

### Step 6: Start Client

```bash
# Using Maven Wrapper (Windows)
.\mvnw.cmd javafx:run

# Linux/Mac
./mvnw javafx:run
```

Or from your IDE:
1. Open `src/main/java/com/auction/client/Launcher.java`
2. Run the main method

---

## Project Structure

```
online-auction-system/
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── client/
│   │   │   │   ├── controller/          # JavaFX UI controllers
│   │   │   │   │   ├── LoginController.java
│   │   │   │   │   ├── AuctionMenuController.java
│   │   │   │   │   ├── AuctionDetailController.java
│   │   │   │   │   ├── AdminMenuController.java
│   │   │   │   │   └── ...
│   │   │   │   ├── service/             # Client-side services (socket wrappers)
│   │   │   │   │   ├── AuthClientService.java
│   │   │   │   │   ├── AuctionClientService.java
│   │   │   │   │   ├── BidClientService.java
│   │   │   │   │   └── ...
│   │   │   │   ├── session/             # Session management
│   │   │   │   │   └── ClientSession.java
│   │   │   │   ├── network/             # Socket communication
│   │   │   │   │   ├── Client.java
│   │   │   │   │   └── ClientConfig.java
│   │   │   │   ├── util/                # Utilities
│   │   │   │   │   ├── FormatUtils.java
│   │   │   │   │   └── SceneUtils.java
│   │   │   │   └── Launcher.java
│   │   │   ├── server/
│   │   │   │   ├── Server.java          # Main server socket listener
│   │   │   │   ├── controller/          # Server-side request handlers
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── AuctionController.java
│   │   │   │   │   ├── BidController.java
│   │   │   │   │   └── ...
│   │   │   │   ├── service/             # Business logic
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── AuctionService.java
│   │   │   │   │   ├── BidService.java
│   │   │   │   │   └── ...
│   │   │   │   ├── repository/          # Database access
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── AuctionRepository.java
│   │   │   │   │   ├── BidRepository.java
│   │   │   │   │   └── ...
│   │   │   │   ├── database/            # Connection pooling
│   │   │   │   │   └── ConnectionPool.java
│   │   │   │   ├── model/               # Server-side entity models
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Auction.java
│   │   │   │   │   ├── Item.java
│   │   │   │   │   └── Bid.java
│   │   │   │   ├── event/               # Event publishing
│   │   │   │   │   ├── AuctionEventPublisher.java
│   │   │   │   │   └── ClientEventDispatcher.java
│   │   │   │   ├── scheduler/           # Scheduled tasks
│   │   │   │   │   └── AuctionStatusScheduler.java
│   │   │   │   └── handler/             # Client connection handlers
│   │   │   │       └── ClientHandler.java
│   │   │   └── shared/
│   │   │       ├── dto/                 # Data Transfer Objects
│   │   │       │   ├── AuctionSummaryDTO.java
│   │   │       │   ├── AuctionDetailDTO.java
│   │   │       │   ├── BidDTO.java
│   │   │       │   ├── UserDTO.java
│   │   │       │   └── ...
│   │   │       ├── protocol/            # Request/Response classes
│   │   │       │   ├── Request.java
│   │   │       │   ├── Response.java
│   │   │       │   ├── ActionType.java
│   │   │       │   └── ...
│   │   │       ├── enums/               # Shared enums
│   │   │       │   ├── AuctionStatus.java
│   │   │       │   ├── Role.java
│   │   │       │   └── ItemType.java
│   │   │       └── exception/           # Custom exceptions
│   │   │           └── ClientServiceException.java
│   │   └── resources/
│   │       ├── db.properties            # Database configuration
│   │       ├── db.properties.example    # Example configuration
│   │       ├── fxml/                    # JavaFX UI layouts
│   │       │   ├── LoginScreen.fxml
│   │       │   ├── AuctionDetailView.fxml
│   │       │   ├── AdminScreen.fxml
│   │       │   └── ...
│   │       ├── css/                     # Stylesheets
│   │       │   └── application.css
│   │       ├── picture/                 # Images
│   │       ├── sql/
│   │       │   └── schema.sql           # Database schema
│   │       └── controller/
│   │           └── example/
│   │               └── demo10/
│   └── test/
│       ├── java/com/auction/
│       │   ├── manual/                  # Manual test flows
│       │   │   └── ManualAuctionFlowTest.java
│       │   └── server/
│       │       └── service/
│       │           └── BidServiceTest.java
│       └── resources/
├── pom.xml                              # Maven configuration
├── mvnw / mvnw.cmd                      # Maven Wrapper
├── README.md                            # Quick start
├── PROTOCOL_CONTRACT.md                 # Client-Server protocol rules
├── CODEBASE_FINAL_REVIEW.md            # Status & known issues
└── PROJECT_DOCUMENTATION.md             # This file
```

---

## Feature Documentation

### 1. Authentication System

#### User Registration
- **UI:** `CreateAccountController` / `createAccount.fxml`
- **Flow:**
  1. User enters username, email, password
  2. Client sends `CREATE_ACCOUNT` request
  3. Server validates unique username/email
  4. Password hashed with BCrypt
  5. User inserted into database
  6. Client receives success and shows login screen

#### User Login
- **UI:** `LoginController` / `LoginScreen.fxml`
- **Flow:**
  1. User enters username and password
  2. Client sends `LOGIN` request
  3. Server validates username exists
  4. Server verifies BCrypt password hash
  5. Server stores session in `ClientHandler`
  6. Client stores user in `ClientSession`
  7. User routed by role:
     - **ADMIN** → `AdminScreen.fxml`
     - **USER** → `AuctionMenu.fxml`

#### User Logout
- **UI:** `AuctionMenuController.handleBack()` / `AdminMenuController.handleLogout()`
- **Flow:**
  1. Client sends `LOGOUT` request
  2. Server clears session
  3. Client clears `ClientSession`
  4. User redirected to login screen

### 2. Auction Management

#### Create Auction
- **UI:** `AuctionMenuController.showCreateAuctionDialog()`
- **Item Types:**
  - **ART:** requires Artist name and Year created
  - **ELECTRONICS:** requires Brand and Warranty months
  - **VEHICLE:** requires Brand, VIN, and Mileage
  - **GENERAL:** basic item only

- **Flow:**
  1. User fills dialog with item details and image
  2. Client sends `CREATE_AUCTION` request with item data
  3. Server creates Item + Auction records
  4. Auction starts in OPEN status
  5. Scheduler monitors start time
  6. When start_time reached → status becomes RUNNING
  7. Scheduler monitors end_time → status becomes FINISHED

#### List Auctions
- **UI:** `AuctionMenuController` with filter/search
- **Endpoints:**
  - `GET_ALL_AUCTIONS`: All auctions with status summary
  - `GET_AUCTIONS_BY_TYPE`: Filter by item type
  - `GET_MY_AUCTIONS`: User's created auctions
  - `GET_MY_PARTICIPATED_AUCTIONS`: Auctions user bid on
  - `GET_MY_WON_AUCTIONS`: User won and needs payment

- **Cards Display:**
  - Item name, image, current price
  - Countdown timer
  - Seller name
  - Bid count
  - Winner name (if finished)

#### View Auction Detail
- **UI:** `AuctionDetailController` / `AuctionDetailView.fxml`
- **Display:**
  - Full item description
  - Starting price, bid step, current price
  - Countdown (different text per status)
  - Seller name
  - Winner name (if finished)
  - Bid history table with pagination
  - **New:** Bid history line chart visualization

- **Real-Time Updates:**
  - Listens to `AUCTION_UPDATED` events
  - Chart updates when new bids placed
  - Table refreshes automatically
  - Countdown timer updates every 1 second

#### Update Auction
- **UI:** `AuctionDetailController.handleUpdateAuction()`
- **Rules:**
  - Only seller can update during OPEN status
  - Can change item name, description, image, and type-specific fields
  - Can change start/end times
- **Flow:**
  1. Dialog shows current values
  2. User modifies fields
  3. Client sends `UPDATE_AUCTION_ITEM` request
  4. Server validates ownership and status
  5. Server updates Item + Auction records
  6. Broadcast updates to all clients

#### Cancel Auction
- **UI:** `AuctionDetailController.handleCancelAuction()`
- **Rules:**
  - **Seller:** Can cancel only during OPEN status
  - **Admin:** Can cancel any time
- **Flow:**
  1. Client sends `CANCEL_AUCTION` request
  2. Server sets status to CANCELED
  3. Broadcast updates to all clients
  4. Bidders' wallets refunded (if implemented)

### 3. Bidding System

#### Place Bid
- **UI:** `AuctionDetailController.handlePlaceBid()`
- **Validation:**
  1. Auction must be RUNNING
  2. Bid amount must be > current price
  3. Bid amount must be ≥ (current price + bid step)
  4. Bidder must have sufficient wallet balance
  5. Bidder cannot bid on own auction

- **Flow:**
  1. User enters bid amount
  2. Client sends `PLACE_BID` request
  3. Server locks auction row
  4. Server validates all above conditions
  5. Server deducts amount from wallet temporarily
  6. Bid inserted into database
  7. Auction.current_price updated
  8. Anti-sniping extension applied if needed
  9. Response includes new bid with serverTime
  10. Broadcast `AUCTION_UPDATED` with latest bid
  11. AuctionDetailController adds bid to table
  12. Chart updates with new data point

#### Anti-Sniping Rule
- **Extension Window:** Last 2 minutes of auction
- **Trigger:** Valid bid placed within this window
- **Action:** End time extended by 2 minutes
- **Implementation:** `BidService.applyAntiSnipingExtension()`

#### Bid History
- **View:** `AuctionDetailController` table
- **Access Methods:**
  - Load via detail page
  - Refresh via "Làm mới" button
  - Auto-update via realtime events

#### Bid History Chart
- **Type:** Line chart (CategoryAxis X, NumberAxis Y)
- **Data:** All bids sorted chronologically
- **Labels:** "Bid 1", "Bid 2", ... "Bid N"
- **Updates:**
  - Full reload when page loads
  - Full reload when new bid placed
  - Single data point added in real-time (alternative without duplicate series)

### 4. Wallet & Payment

#### View Balance
- **UI:** `AuctionMenuController` top-right label
- **Updates:** Refreshed after deposit/payment

#### Deposit Funds
- **UI:** `AuctionMenuController.showDepositDialog()`
- **Flow:**
  1. User enters deposit amount
  2. Dialog validation (must be positive)
  3. Client sends `ADD_BALANCE` request
  4. Server adds amount to user's balance
  5. Response contains new balance
  6. ClientSession updates
  7. UI label refreshes

#### Pay Auction Winner
- **UI:** Card in "My Won Auctions" section
- **Rules:**
  - Only winner of FINISHED auction can pay
  - Payment moves status to PAID
  - Balance must be sufficient

- **Flow:**
  1. User clicks pay on won auction
  2. Client sends `PAY_AUCTION` request
  3. Server validates winner + status
  4. Server deducts final price from balance
  5. Status changed to PAID
  6. Broadcast updates
  7. Auction card disappears from "My Won" list

### 5. User Management (Admin)

#### View All Users
- **UI:** `AdminMenuController` / `AdminUsersViewController`
- **Table Columns:**
  - Username
  - Email
  - Role (ADMIN/USER)
  - Balance
  - Ban status
  - Actions

#### Ban User
- **UI:** Ban button on user row (not visible for ADMIN accounts)
- **Rules:**
  - Cannot ban ADMIN accounts
  - Banned users cannot login
  - Ban can be permanent or timed

- **Flow:**
  1. Admin clicks BAN
  2. Dialog shows ban duration options
  3. Client sends `BAN_USER` request
  4. Server applies ban_start_time and ban_end_time
  5. If banned user is online, disconnects them
  6. Table refreshes

#### Unban User
- **UI:** Unban button on banned user row
- **Flow:**
  1. Admin clicks UNBAN
  2. Client sends `UNBAN_USER` request
  3. Server clears ban times
  4. User can login again

#### View Auction Details (Admin)
- **UI:** Same as user detail view
- **Additional:** Admin action buttons (update, cancel)

---

## Database Schema

### Users Table

```sql
CREATE TABLE `users` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,      -- BCrypt hash
  `profile_picture` VARCHAR(255),
  `role` ENUM('ADMIN','USER') DEFAULT 'USER',
  `balance` DECIMAL(15,2) DEFAULT 0.00,
  `ban_start_time` BIGINT DEFAULT 0,     -- milliseconds
  `ban_end_time` BIGINT DEFAULT 0,       -- milliseconds
  `create_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`)
);
```

### Items Table

```sql
CREATE TABLE `items` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `owner_id` INT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `image_url` VARCHAR(255),               -- base64 or file path
  `start_price` DECIMAL(15,2) NOT NULL,
  `item_type` ENUM('GENERAL','ART','VEHICLE','ELECTRONICS') NOT NULL,
  
  -- Type-specific fields
  `brand` VARCHAR(255),                   -- VEHICLE, ELECTRONICS
  `warranty_months` INT,                  -- ELECTRONICS
  `artist` VARCHAR(255),                  -- ART
  `creation_year` INT,                    -- ART
  `vin` VARCHAR(100),                     -- VEHICLE
  `mileage` INT,                          -- VEHICLE
  
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `fk_item_owner` (`owner_id`),
  CONSTRAINT `fk_item_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
);
```

### Auctions Table

```sql
CREATE TABLE `auctions` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `item_id` INT NOT NULL,
  `starting_price` DECIMAL(15,2) NOT NULL,
  `bid_step` DECIMAL(15,2) NOT NULL,
  `current_price` DECIMAL(15,2) NOT NULL,
  `last_bidder_id` INT,
  `start_time` TIMESTAMP NOT NULL,
  `end_time` TIMESTAMP NOT NULL,
  `status` ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED') DEFAULT 'OPEN',
  `winner_id` INT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `fk_auction_item` (`item_id`),
  KEY `fk_auction_last_bidder` (`last_bidder_id`),
  KEY `fk_auction_winner` (`winner_id`),
  KEY `idx_auction_status` (`status`),
  CONSTRAINT `fk_auction_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE
);
```

### Bids Table

```sql
CREATE TABLE `bids` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `auction_id` INT NOT NULL,
  `bidder_id` INT NOT NULL,
  `bid_amount` DECIMAL(15,2) NOT NULL,
  `bid_time` TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
  KEY `fk_bid_auction` (`auction_id`),
  KEY `fk_bid_user` (`bidder_id`),
  CONSTRAINT `fk_bid_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bid_user` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);
```

---

## API & Protocol Documentation

### ActionType Enum

Core actions supported by the system:

```java
// Authentication
LOGIN
CREATE_ACCOUNT
LOGOUT
BAN_USER
UNBAN_USER

// Auctions
CREATE_AUCTION
GET_ALL_AUCTIONS
GET_AUCTIONS_BY_TYPE
GET_AUCTION_DETAIL
GET_MY_AUCTIONS
GET_MY_PARTICIPATED_AUCTIONS
GET_MY_WON_AUCTIONS
UPDATE_AUCTION_ITEM
CANCEL_AUCTION

// Bids
PLACE_BID
GET_BID_HISTORY_BY_AUCTION
GET_MY_BIDS

// Wallet
ADD_BALANCE
PAY_AUCTION

// Realtime Events
AUCTION_UPDATED
```

### Request/Response Pattern

Every server communication follows:

```java
// Client sends
Request<T> request = new Request<>(ActionType.ACTION_NAME, payload);
client.sendRequest(request);

// Server responds
Response<R> response = new Response<>(action, status, payload, errorMessage);
```

### Example: Place Bid Request/Response

**Request:**
```java
public class PlaceBidRequest implements Serializable {
    private int auctionId;
    private BigDecimal amount;
}

Request<PlaceBidRequest> request = 
    new Request<>(ActionType.PLACE_BID, 
        new PlaceBidRequest(123, new BigDecimal("50.00")));
```

**Response:**
```java
public class PlaceBidResponse implements Serializable {
    private String message;
    private BidDTO bid;
}

Response<PlaceBidResponse> response = 
    Response.success(ActionType.PLACE_BID, 
        new PlaceBidResponse("Bid placed successfully", bidDTO));
```

### Real-Time Events

When an auction changes, server broadcasts to all connected clients:

```java
public class AuctionUpdatedEvent implements Serializable {
    private int auctionId;
    private AuctionSummaryDTO summary;        // Current state
    private AuctionUpdateType updateType;     // BID_PLACED, AUCTION_STARTED, etc.
    private BidDTO latestBid;                 // Included if BID_PLACED
}

// Client receives
Response<AuctionUpdatedEvent> event = 
    new Response<>(ActionType.AUCTION_UPDATED, Status.SUCCESS, new AuctionUpdatedEvent(...));

// AuctionDetailController listener handles it
auctionUpdatedListener = response -> {
    AuctionUpdatedEvent event = (AuctionUpdatedEvent) response.getPayload();
    if (event.getAuctionId() == currentAuctionId) {
        applyRealtimeSummary(event);  // Update UI
    }
};
```

---

## Client-Side Architecture

### Package: `client.controller`

| Class | Purpose |
|-------|---------|
| `LoginController` | Login form, navigation to register |
| `CreateAccountController` | Registration form |
| `AuctionMenuController` | Main auction list, filters, cards, modals |
| `AuctionDetailController` | Auction detail, bid placement, realtime updates, chart |
| `AdminMenuController` | Admin dashboard, auction table |
| `AdminUsersViewController` | User management table, ban/unban |

### Package: `client.service`

Client-side services wrap Socket communication:

| Class | Methods |
|-------|---------|
| `AuthClientService` | `login()`, `register()`, `logout()` |
| `AuctionClientService` | `getAuctions()`, `getAuctionDetail()`, `createAuction()`, `updateAuctionItem()`, `cancelAuction()` |
| `BidClientService` | `placeBid()`, `getBidHistoryByAuction()`, `getMyBids()` |
| `UserClientService` | `getUsers()`, `banUser()`, `unbanUser()` |
| `FinanceClientService` | `addBalance()`, `payAuction()` |

### Package: `client.session`

```java
class ClientSession {
    private static UserDTO currentUser;
    
    public static UserDTO getCurrentUser();
    public static void setCurrentUser(UserDTO user);
    public static void logout();
    public static boolean isLoggedIn();
}
```

### Package: `client.network`

```java
class Client {
    private static Client instance;
    
    public <T, R> Response<R> sendRequest(Request<T> request);
    public void addEventListener(ActionType type, Consumer<Response<?>> listener);
    public void removeEventListener(ActionType type, Consumer<Response<?>> listener);
    public void disconnect();
}
```

### JavaFX UI Lifecycle

Each controller:
1. **initialize()** - Set up UI components, listeners
2. **setAuctionId() / setInitialAuction()** - Load data asynchronously
3. **Task-based loading** - Long operations run on background thread
4. **Platform.runLater()** - UI updates on JavaFX Thread
5. **cleanup()** - Unregister listeners before scene close

Example:
```java
@FXML
private void initialize() {
    setupBidHistoryTable();
    setupButtonActions();
    startCountdownTimeline();  // Update every 1 second
    root.sceneProperty().addListener((obs, oldScene, newScene) -> {
        if (newScene == null) cleanup();  // Cleanup on close
    });
}

public void setAuctionId(int auctionId) {
    currentAuctionId = auctionId;
    loadAuctionDetailPageAsync(false);    // Background load
    registerRealtimeListener();            // Start listening
}

private void cleanup() {
    unregisterRealtimeListener();
    stopCountdownTimeline();
}
```

---

## Server-Side Architecture

### Package: `server.Server`

Main server class:
- Listens on port 5000 (configurable)
- Accepts client connections
- Creates `ClientHandler` for each connection
- Maintains map of logged-in `ClientHandler` instances

```java
public class Server {
    private static ServerSocket serverSocket;
    private static Map<Integer, ClientHandler> loggedInClients;
    
    public static void main(String[] args) {
        serverSocket = new ServerSocket(5000);
        while (true) {
            Socket client = serverSocket.accept();
            ClientHandler handler = new ClientHandler(client);
            new Thread(handler).start();
        }
    }
}
```

### Package: `server.handler`

`ClientHandler` manages:
- Socket I/O (ObjectInputStream/ObjectOutputStream)
- Logged-in user session
- Request dispatching
- Event broadcasting

```java
public class ClientHandler implements Runnable {
    private UserDTO currentUser;
    
    @Override
    public void run() {
        while (true) {
            Request<?> request = readRequest();
            Response<?> response = dispatcher.handle(request, this);
            sendResponse(response);
        }
    }
    
    public void sendEvent(Response<?> event) {
        // Push realtime event to this client
    }
}
```

### Package: `server.controller`

Request handlers:

| Class | Handles |
|-------|---------|
| `AuthController` | LOGIN, CREATE_ACCOUNT, LOGOUT, BAN_USER, UNBAN_USER |
| `AuctionController` | Create, list, detail, update, cancel auctions |
| `BidController` | Place bid, get history |
| `AdminController` | Admin actions (bans, etc.) |

Example:
```java
public class AuctionController {
    public Response<AuctionDetailDTO> handleGetAuctionDetail(
            ClientHandler handler, 
            Request<Integer> request) {
        int auctionId = request.getPayload();
        AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);
        return Response.success(ActionType.GET_AUCTION_DETAIL, detail);
    }
}
```

### Package: `server.service`

Business logic layer:

| Class | Responsibilities |
|-------|------------------|
| `AuthService` | Password hashing, login validation, registration |
| `AuctionService` | Auction CRUD, status management, lifecycle |
| `BidService` | Bid validation, placement, anti-sniping |
| `UserService` | User queries, ban/unban, balance updates |
| `WalletService` | Balance transactions |
| `PaymentService` | Winner payment processing |

All use transactions and database locks for concurrent safety.

### Package: `server.repository`

Database access layer using JDBC:

| Class | Queries |
|-------|---------|
| `UserRepository` | Find user, create, update balance, ban |
| `AuctionRepository` | CRUD auctions, status queries, filtering |
| `BidRepository` | Insert bid, query by auction/user |
| `ItemRepository` | CRUD items |

### Package: `server.scheduler`

`AuctionStatusScheduler`:
- Runs every 1 second
- Checks all OPEN auctions for start_time
- Checks all RUNNING auctions for end_time
- Updates status and broadcasts events

```java
public class AuctionStatusScheduler {
    public void run() {
        // Check OPEN -> RUNNING transitions
        List<Auction> started = auctionRepo.findByStartTimeExpired();
        for (Auction a : started) {
            a.setStatus(AuctionStatus.RUNNING);
            publisher.publishAuctionStarted(a);
        }
        
        // Check RUNNING -> FINISHED transitions
        List<Auction> finished = auctionRepo.findByEndTimeExpired();
        for (Auction a : finished) {
            a.setStatus(AuctionStatus.FINISHED);
            a.setWinnerId(a.getLastBidderId());
            publisher.publishAuctionFinished(a);
        }
    }
}
```

### Package: `server.event`

Event publishing:

```java
public class AuctionEventPublisher {
    public void publishBidPlaced(Auction auction, Bid bid) {
        AuctionUpdatedEvent event = new AuctionUpdatedEvent(
            auction.getId(),
            auctionService.summarize(auction),
            AuctionUpdateType.BID_PLACED,
            bid.toDTO()
        );
        dispatchToClients(event);
    }
}

public class ClientEventDispatcher {
    public void broadcastToLoggedIn(Response<?> response) {
        for (ClientHandler client : Server.getLoggedInClients().values()) {
            client.sendEvent(response);
        }
    }
}
```

---

## User Guides

### Login Flow

1. **Start Application** → LoginScreen displays
2. **Enter Credentials** → Username and encrypted password
3. **Click Login** → Client sends request to server
4. **Server Validates** → Checks username, BCrypt hash, ban status
5. **Success:**
   - ADMIN → AdminScreen
   - USER → AuctionMenu
6. **Failure** → Error message displays, retry login

### Creating an Auction

1. **From AuctionMenu** → Click "Tạo phiên đấu giá" button
2. **Choose Item Type** → Select ART, ELECTRONICS, VEHICLE, or GENERAL
3. **Fill Details:**
   - Item name, description
   - Starting price, bid step
   - Type-specific fields (Artist, Brand, VIN, etc.)
   - Upload image (PNG, JPG)
4. **Set Schedule:**
   - Start time (when bidding opens)
   - End time (when bidding closes)
5. **Confirm** → Auction created in OPEN status
6. **Wait** → Scheduler transitions to RUNNING at start_time

### Placing a Bid

1. **View Auction** → Click on auction card
2. **Check Status** → Auction must be RUNNING
3. **Enter Bid Amount** → Must be > current price + bid step
4. **Confirm Balance** → Display shows available funds
5. **Click "Đặt giá"** → Sends request to server
6. **Server Validates:**
   - Auction still RUNNING
   - Amount valid
   - Balance sufficient
7. **Success:** Chart updates, countdown extends if in last 2 min
8. **Failure:** Error message explains rejection

### Winning & Paying

1. **When Auction Ends** → Status changes to FINISHED
2. **If You Won:**
   - "Đấu giá đã thắng" section shows your won auctions
   - Click "Thanh toán" to pay
3. **Payment Process:**
   - Server validates you're the winner
   - Deducts final price from wallet
   - Status changes to PAID
4. **Auction Removed** → From "Won" list after payment

### Depositing Funds

1. **From AuctionMenu** → Click "Nạp tiền"
2. **Enter Amount** → Must be positive
3. **Confirm** → Balance updated immediately
4. **Display Refreshes** → Top-right balance shows new amount

### Admin: Managing Users

1. **Login as ADMIN** → Routes to AdminScreen
2. **See Users Table** → All registered users displayed
3. **Ban User:**
   - Click BAN on user row
   - Select ban duration
   - Confirm
   - User cannot login
4. **Unban User:**
   - Click UNBAN on banned user
   - User can login again

### Admin: Managing Auctions

1. **AdminScreen → Auctions Tab**
2. **View All Auctions** → Loaded in background
3. **Filter/Search** → By type, status, seller
4. **Click "Xem chi tiết"** → View detail (same as user)
5. **Admin Actions** → UPDATE or CANCEL buttons
   - Can update ANY OPEN auction
   - Can cancel ANY RUNNING/OPEN auction

---

## Development Guidelines

### 1. Client-Server Protocol Rules

**CRITICAL RULE:** Client cannot import or call `com.auction.server.service.*` or `com.auction.server.model.*`

**Correct Flow:**
```
JavaFX Controller → ClientService → Request → Socket → Server → Response → Controller
```

**Incorrect Flow (NEVER do this):**
```
JavaFX Controller → AuctionService.getInstance().getAuctions()  // WRONG!
```

See `PROTOCOL_CONTRACT.md` for full rules.

### 2. Adding a New Feature

1. **Define ActionType** in `shared.protocol.ActionType`:
   ```java
   public enum ActionType {
       // ... existing
       NEW_FEATURE  // Add here
   }
   ```

2. **Create Request DTO** if needed:
   ```java
   public class NewFeatureRequest implements Serializable {
       // Fields
   }
   ```

3. **Create Response DTO** if needed:
   ```java
   public class NewFeatureResponse implements Serializable {
       // Fields
   }
   ```

4. **Create Client Service** method:
   ```java
   // client/service/SomeClientService.java
   public SomeResult newFeature(...) {
       Request<NewFeatureRequest> request = 
           new Request<>(ActionType.NEW_FEATURE, payload);
       Response<NewFeatureResponse> response = client.sendRequest(request);
       if (!response.isSuccess()) throw new ClientServiceException(...);
       return response.getPayload();
   }
   ```

5. **Add Controller Handler**:
   ```java
   // server/controller/SomeController.java
   public Response<NewFeatureResponse> handleNewFeature(
           ClientHandler handler, 
           Request<NewFeatureRequest> request) {
       UserDTO user = handler.getCurrentUser();
       // Validate
       NewFeatureResponse result = someService.newFeature(user, request.getPayload());
       return Response.success(ActionType.NEW_FEATURE, result);
   }
   ```

6. **Add Server Service Logic**:
   ```java
   // server/service/SomeService.java
   public NewFeatureResponse newFeature(UserDTO user, NewFeatureRequest req) {
       // Business logic
       // Database access
       // Return response
   }
   ```

7. **Update Dispatcher**:
   ```java
   // server/handler/ClientHandler.java (or dispatcher pattern)
   case NEW_FEATURE -> 
       someController.handleNewFeature(this, (Request<?>) request);
   ```

8. **Call from UI**:
   ```java
   // client/controller/SomeController.java
   someClientService.newFeature(...);
   ```

### 3. Async Task Pattern

For long operations, use JavaFX `Task`:

```java
Task<List<AuctionDTO>> task = new Task<>() {
    @Override
    protected List<AuctionDTO> call() {
        return auctionClientService.getAuctions();
    }
};

task.setOnSucceeded(event -> {
    List<AuctionDTO> auctions = task.getValue();
    // Update UI on JavaFX thread
    renderAuctions(auctions);
});

task.setOnFailed(event -> {
    showError(errorMessage(task.getException()));
});

Thread thread = new Thread(task);
thread.setDaemon(true);
thread.start();
```

### 4. Real-Time Event Listening

Pattern for listening to realtime updates:

```java
private Consumer<Response<?>> auctionUpdatedListener;
private boolean realtimeListenerRegistered = false;

private void registerRealtimeListener() {
    if (realtimeListenerRegistered) return;
    
    auctionUpdatedListener = response -> {
        if (response.getAction() != ActionType.AUCTION_UPDATED) return;
        Object payload = response.getPayload();
        if (!(payload instanceof AuctionUpdatedEvent event)) return;
        if (event.getAuctionId() != currentAuctionId) return;
        
        // Handle event
        applyRealtimeSummary(event);
    };
    
    client.addEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
    realtimeListenerRegistered = true;
}

private void unregisterRealtimeListener() {
    if (realtimeListenerRegistered && auctionUpdatedListener != null) {
        client.removeEventListener(ActionType.AUCTION_UPDATED, auctionUpdatedListener);
    }
    realtimeListenerRegistered = false;
}

@FXML
private void initialize() {
    // ...cleanup on scene close
    root.sceneProperty().addListener((obs, oldScene, newScene) -> {
        if (oldScene != null && newScene == null) {
            cleanup();
        }
    });
}

public void cleanup() {
    unregisterRealtimeListener();
}
```

### 5. Database Transactions

Use row locks for concurrent safety:

```java
// server/service/BidService.java
try (Connection conn = ConnectionPool.getConnection()) {
    conn.setAutoCommit(false);
    
    // Lock the auction row
    Auction auction = auctionRepo.findById(auctionId, conn);
    // ... select for update happens automatically in many DBs
    
    // Validate
    if (auction.getCurrentPrice() >= amount) {
        conn.rollback();
        throw new InsufficientBidException(...);
    }
    
    // Update
    bidRepo.insert(bid, conn);
    auctionRepo.updateCurrentPrice(auctionId, amount, conn);
    
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
}
```

### 6. Error Handling

- **Client:** Catch `ClientServiceException`, display to user
- **Server:** Return `Response.error()` with message
- **Never:** Expose stack traces to client

```java
// Client
try {
    auctionClientService.placeBid(auctionId, amount);
} catch (ClientServiceException e) {
    showError(e.getMessage());  // User-friendly message
}

// Server
public Response<PlaceBidResponse> handlePlaceBid(...) {
    try {
        bidService.placeBid(...);
        return Response.success(...);
    } catch (InvalidBidException e) {
        return Response.error(ActionType.PLACE_BID, e.getMessage());
    }
}
```

### 7. Styles & Formatting

- **Language:** Vietnamese UI text (in the spirit of original design)
- **Currency:** Use `FormatUtils.currency()` for prices
- **Time:** Use `DateTimeFormatter` for timestamps
- **Naming:** Camel case for Java, matching Vietnamese semantics where possible

---

## Testing

### Unit Tests

Located in `src/test/java/`:

```java
public class BidServiceTest {
    @Test
    public void placeBid_whenValidBid_shouldCreateBidAndUpdateAuction() {
        // Arrange
        int auctionId = 1;
        BigDecimal amount = new BigDecimal("50.00");
        
        // Act
        BidDTO result = bidService.placeBid(userId, auctionId, amount);
        
        // Assert
        assertNotNull(result);
        assertEquals(auctionId, result.getAuctionId());
        assertEquals(amount.doubleValue(), result.getAmount(), 0.01);
    }
}
```

### Running Tests

```bash
# Windows
.\mvnw.cmd test

# Linux/Mac
./mvnw test
```

### Current Test Status

- **Total Tests:** 11
- **Passed:** 11
- **Failed:** 0
- **Skipped:** 1 (requires OPEN/RUNNING auction in DB)

Test execution requires:
- MySQL database running
- Schema initialized
- Data fixtures in place

---

## Known Issues & Risks

### Priority 1 (Should Fix Before Production)

#### Read-Time Status Finalization Race Condition
- **Issue:** `AuctionRepository.finalizeExpiredAuctionsForRead()` is called in get methods, but the scheduler is the only source of broadcasts
- **Risk:** Two clients see different status if one triggers finalization without broadcast
- **Suggested Fix:** Centralize finalization in scheduler only, return changed summaries

**Location:** `AuctionService.getAllAuctions()`, `AuctionRepository`

### Priority 2 (Should Fix Before Demo)

#### Client Request Correlation by ActionType Only
- **Issue:** `Client.pendingResponses: Map<ActionType, BlockingQueue<Response<?>>>`
- **Risk:** Two simultaneous requests of same type (e.g., two PLACE_BID) may get mismatched responses
- **Workaround:** Disable button during request to prevent double-click
- **Suggested Fix:** Add requestId/correlationId to Request/Response protocol

#### `AuctionUpdatedEvent.lastestBid` Typo
- **Issue:** Field is `lastestBid` (typo), alias getter `getLatestBid()` exists
- **Risk:** Confusion during refactoring
- **Not Fixed:** Renaming requires coordinated client/server rebuild
- **Workaround:** Use `getLatestBid()` always

### Priority 3 (Nice to Have)

#### Double Used for Money Values
- **Issue:** `BigDecimal` for UI, but service/repository use `double`
- **Risk:** Floating-point rounding errors
- **Suggested Fix:** Use `BigDecimal` throughout post-demo

#### Tests Depend on Live Database
- **Issue:** Tests read from current DB; missing fixtures cause test skip
- **Suggested Fix:** Isolated test DB or transactional setup/teardown

#### No Realtime Updates for Admin Screen
- **Issue:** AdminScreen reloads table on demand, doesn't listen to AUCTION_UPDATED
- **Suggested Fix:** Add AUCTION_UPDATED listener if admin table must live-update

#### Server Logging is Verbose
- **Issue:** `System.out.println()` per broadcast/scheduler event
- **Suggested Fix:** Replace with logger, reduce INFO volume post-demo

---

## Troubleshooting

### Client Cannot Connect to Server

**Symptom:** "Connection refused" error on login

**Solutions:**
1. Verify server is running (should print "Server is listening on port 5000")
2. Check `ClientConfig.SERVER_ADDRESS` matches server hostname/IP
3. Check firewall allows port 5000
4. Check MySQL is running if server crashes on startup

### Login Fails with "User not found"

**Symptom:** Correct username but login rejected

**Solutions:**
1. User was not registered (create new account first)
2. Username is case-sensitive
3. Check `users` table: `SELECT * FROM users WHERE username='admin';`
4. Try default admin: username=`admin`, password=`admin`

### Bid Placement Fails with "Insufficient funds"

**Symptom:** "Insufficient balance" error despite depositing money

**Solutions:**
1. Verify deposit was successful (check balance label)
2. Bid amount must be > current_price + bid_step
3. Balance is temporary hold during bid processing; verify after confirmation
4. Withdraw logic not implemented; "reserved" funds are not returned if bid fails

### Auction Chart Shows No Data

**Symptom:** Line chart is empty even with multiple bids

**Solutions:**
1. Verify bids exist in table (refresh table manually)
2. Chart may require more than 1 data point to render
3. Check `AuctionDetailView.fxml` has `<fx:id>lineChart</fx:id>`
4. Check imports in controller include `javafx.scene.chart.*`

### Real-Time Updates Not Working

**Symptom:** Other client's bids don't appear automatically

**Solutions:**
1. Verify both clients on same network
2. Check real-time listener registered: `realtimeListenerRegistered` flag
3. Manually refresh table with "Làm mới" button (polls server)
4. Check server broadcasting (search logs for "AUCTION_UPDATED")

### Database Connection Timeout

**Symptom:** "Connection timeout" error after period of inactivity

**Solutions:**
1. Check MySQL is still running
2. Connection pool may have timed out; restart server
3. Increase `db.properties` connection timeout (default 30s)
4. Check firewall allows MySQL port 3306

### Compilation Errors

**Symptom:** "Cannot find symbol" or "package does not exist"

**Solutions:**
```bash
# Clean and rebuild
.\mvnw.cmd clean compile

# Check dependencies
.\mvnw.cmd dependency:tree

# Verify JDK version
java -version  # Should be 21+
```

### Out of Memory Errors

**Symptom:** "OutOfMemoryException" after loading large auction list

**Solutions:**
1. Reduce number of items per page (implement pagination)
2. Optimize image loading (compress before upload)
3. Increase JVM heap: `java -Xmx1g com.auction.server.Server`

---

## Summary

This Online Auction System implements a **socket-based Client-Server architecture** with:

- **JavaFX desktop client** for interactive bidding
- **Multi-threaded server** handling concurrent clients
- **MySQL database** for persistent storage
- **Real-time event broadcasting** for live updates
- **Anti-sniping auction extension** for fairness
- **Role-based access control** for admin functions
- **Wallet system** for secure balance management

The codebase follows strict client-server separation:
- Client never imports server internals
- Communication via Request/Response protocol
- DTO objects bridge client and server
- Server session management ensures security

For questions or issues, refer to `CODEBASE_FINAL_REVIEW.md` for current status and `PROTOCOL_CONTRACT.md` for protocol design rules.

---

**End of Documentation**

Last Updated: June 3, 2026

