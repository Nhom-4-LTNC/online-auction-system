-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: 172.29.240.1    Database: online_auction_db
-- ------------------------------------------------------
-- Server version	9.7.0
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO,ANSI' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '0cf05cbd-48fe-11f1-86ba-40c2baafa02c:1-54';

--
-- Table structure for table "auctions"
--

DROP TABLE IF EXISTS "auctions";
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE "auctions" (
  "id" int NOT NULL AUTO_INCREMENT,
  "item_id" int NOT NULL,
  "start_prie" decimal(15,2) NOT NULL,
  "current_price" decimal(15,2) NOT NULL,
  "bid_step" decimal(15,2) NOT NULL,
  "start_time" datetime NOT NULL,
  "end_time" datetime NOT NULL,
  "status" enum('OPEN','RUNNING','FINISHED','PAID','CANCELED') DEFAULT 'OPEN',
  "winer_id" int DEFAULT NULL,
  "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  KEY "item_id" ("item_id"),
  KEY "winer_id" ("winer_id"),
  KEY "idx_auction_status" ("status"),
  CONSTRAINT "auctions_ibfk_1" FOREIGN KEY ("item_id") REFERENCES "items" ("id") ON DELETE CASCADE,
  CONSTRAINT "auctions_ibfk_2" FOREIGN KEY ("winer_id") REFERENCES "users" ("id") ON DELETE SET NULL
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table "auctions"
--

LOCK TABLES "auctions" WRITE;
/*!40000 ALTER TABLE "auctions" DISABLE KEYS */;
/*!40000 ALTER TABLE "auctions" ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table "bid_transactions"
--

DROP TABLE IF EXISTS "bid_transactions";
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE "bid_transactions" (
  "id" int NOT NULL AUTO_INCREMENT,
  "auction_id" int NOT NULL,
  "user_id" int NOT NULL,
  "amount" decimal(15,2) NOT NULL,
  "bid_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  KEY "user_id" ("user_id"),
  KEY "inx_bid_auction_id" ("auction_id"),
  CONSTRAINT "bid_transactions_ibfk_1" FOREIGN KEY ("auction_id") REFERENCES "auctions" ("id") ON DELETE CASCADE,
  CONSTRAINT "bid_transactions_ibfk_2" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table "bid_transactions"
--

LOCK TABLES "bid_transactions" WRITE;
/*!40000 ALTER TABLE "bid_transactions" DISABLE KEYS */;
/*!40000 ALTER TABLE "bid_transactions" ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table "items"
--

DROP TABLE IF EXISTS "items";
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE "items" (
  "id" int NOT NULL AUTO_INCREMENT,
  "owner_id" int NOT NULL,
  "name" varchar(255) NOT NULL,
  "start_price" decimal(15,2) NOT NULL,
  "item_type" enum('GENERAL','ART','VEHICLE','ELECTRONICS') NOT NULL,
  "specific_attributes" json DEFAULT NULL,
  "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  KEY "owner_id" ("owner_id"),
  CONSTRAINT "items_ibfk_1" FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON DELETE CASCADE
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table "items"
--

LOCK TABLES "items" WRITE;
/*!40000 ALTER TABLE "items" DISABLE KEYS */;
/*!40000 ALTER TABLE "items" ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table "users"
--

DROP TABLE IF EXISTS "users";
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE "users" (
  "id" int NOT NULL AUTO_INCREMENT,
  "username" varchar(50) NOT NULL,
  "email" varchar(150) NOT NULL,
  "password" varchar(255) NOT NULL,
  "role" enum('ADMIN','USER') DEFAULT 'USER',
  "balance" decimal(15,2) DEFAULT '0.00',
  "create_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  UNIQUE KEY "username" ("username"),
  UNIQUE KEY "email" ("email")
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table "users"
--

LOCK TABLES "users" WRITE;
/*!40000 ALTER TABLE "users" DISABLE KEYS */;
/*!40000 ALTER TABLE "users" ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-09 23:53:49
