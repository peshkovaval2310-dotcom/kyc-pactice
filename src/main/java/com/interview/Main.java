package com.interview;

import com.interview.repository.NotificationTaskRepository;
import com.interview.sms.NotificationClient;
import com.interview.worker.NotificationScheduler;
import com.interview.worker.NotificationWorker;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        NotificationTaskRepository taskRepository =  null;
        NotificationClient notificationClient =  null;

        NotificationWorker worker =
                new NotificationWorker(taskRepository, notificationClient);
        NotificationScheduler scheduler = new NotificationScheduler(worker, 10);

        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::close));

        scheduler.start();
        Thread.currentThread().join();
    }
}