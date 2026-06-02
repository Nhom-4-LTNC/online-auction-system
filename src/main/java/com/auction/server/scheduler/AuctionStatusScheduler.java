package com.auction.server.scheduler;

import com.auction.server.event.AuctionEventPublisher;
import com.auction.server.service.AuctionService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionStatusScheduler {
    private static final AuctionStatusScheduler INSTANCE = new AuctionStatusScheduler();

    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionEventPublisher eventPublisher = AuctionEventPublisher.getInstance();

    private ScheduledExecutorService executor;

    private AuctionStatusScheduler() {}

    public static AuctionStatusScheduler getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auction-status-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
        System.out.println("[AuctionStatusScheduler] Started.");
    }

    public synchronized void stop() {
        if (executor == null) {
            return;
        }

        executor.shutdownNow();
        executor = null;
        System.out.println("[AuctionStatusScheduler] Stopped.");
    }

    private void tick() {
        try {
            List<AuctionService.AuctionStatusChangeResult> changes =
                    auctionService.refreshDueAuctionStatuses();
            for (AuctionService.AuctionStatusChangeResult change : changes) {
                eventPublisher.publishAuctionUpdated(
                        change.auctionId(),
                        change.updateType(),
                        change.summary(),
                        null,
                        statusMessage(change)
                );
                System.out.println("[AuctionStatusScheduler] auctionId=" + change.auctionId()
                        + " status=" + change.newStatus());
            }
        } catch (Exception e) {
            System.err.println("[AuctionStatusScheduler] Tick failed: " + e.getMessage());
        }
    }

    private String statusMessage(AuctionService.AuctionStatusChangeResult change) {
        return switch (change.updateType()) {
            case AUCTION_STARTED -> "Phiên đấu giá đã bắt đầu.";
            case AUCTION_FINISHED -> "Phiên đấu giá đã kết thúc.";
            default -> "Trạng thái phiên đấu giá đã được cập nhật.";
        };
    }
}
