package com.interview.repository;

import com.interview.model.NotificationTask;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationTaskRepository { ;
    List<NotificationTask> claimReadyTask(
            Instant now,
            Instant lockedUntil,
            int limit
    );

    boolean markSent(UUID taskId, UUID claimToken, Instant sentAt);

    boolean scheduleRetry(
            UUID taskId,
            UUID claimToken,
            Instant nextAttemptAt,
            String lastError
    );
    boolean markFailed(UUID taskId, UUID claimToken, String lastError);


}
