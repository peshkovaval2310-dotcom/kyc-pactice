package com.interview.worker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler implements AutoCloseable {

    private final NotificationWorker worker;
    private final long delaySeconds;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    public NotificationScheduler(NotificationWorker worker, long delaySeconds) {
        this.worker = worker;
        this.delaySeconds = delaySeconds;
    }

    public void start() {
        executor.scheduleWithFixedDelay(
                this::runSafely, 0, delaySeconds, TimeUnit.SECONDS
        );
    }

    private void runSafely() {
        try {
            worker.runOnce();
        } catch (RuntimeException exception) {
            System.err.println("Notification pass failed: " + exception);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}