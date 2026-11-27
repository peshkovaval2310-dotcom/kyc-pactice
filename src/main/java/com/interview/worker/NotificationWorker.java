package com.interview.worker;

import com.interview.model.NotificationTask;
import com.interview.repository.NotificationTaskRepository;
import com.interview.sms.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class NotificationWorker {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(60);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(2);
    private final NotificationTaskRepository taskRepository;
    private final NotificationClient notificationClient;

    public NotificationWorker(NotificationTaskRepository taskRepository, NotificationClient notificationClient) {
        this.taskRepository = taskRepository;
        this.notificationClient = notificationClient;
    }

    public void runOnce() {
        Instant now = Instant.now();

        List<NotificationTask> tasks = taskRepository.claimReadyTask(
                now,
                now.plus(LOCK_DURATION),
                BATCH_SIZE
        );
        for (NotificationTask task : tasks) {
            try {
                process((task));
            } catch (RuntimeException exception) {
                System.err.println("Не удалось обработать задание "
                        + task.id() + ": " + exception
                );
            }
        }
    }

    private void process(NotificationTask task) {
        SmsRequest request = new SmsRequest(
                task.clientId().toString(),
                task.text()
        );
        NotificationResult result;

        try {
            result = notificationClient.sendSms(request);
        } catch (RuntimeException exception) {

            retryOrFail(task, exception.toString());
            return;
        }
        if (result instanceof Success) {
            taskRepository.markSent(
                    task.id(),
                    task.claimToken(),
                    Instant.now()
            );
        } else if (result instanceof ValidationError) {
            taskRepository.markFailed(
                    task.id(),
                    task.claimToken(),
                    "SMS request validation failed"

            );
        } else {
            retryOrFail(
                    task,
                    "Notification client returned an unexpected result"
            );
        }
    }

    private void retryOrFail(NotificationTask task, String error) {
        if (task.attempts() >= MAX_ATTEMPTS) {
            taskRepository.markFailed(
                    task.id(),
                    task.claimToken(),
                    error
            );
            return;
        }
        taskRepository.scheduleRetry(
                task.id(),
                task.claimToken(),
                Instant.now().plus(RETRY_DELAY),
                error
        );

    }

}
