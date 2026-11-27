package com.interview.model;

import com.interview.sms.NotificationTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationTask(
        UUID id,
        UUID applicationId,
        UUID clientId,
        String text,
        NotificationTaskStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant lockedUntil,
        UUID claimToken,
        Instant sentAt
) {
    public static NotificationTask pendingKycCompleteSms(Application application){
        return new NotificationTask(
                UUID.randomUUID(),
                application.id(),
                application.clientId(),
                "Проверка личности успешно завершена.",
                NotificationTaskStatus.PENDING,
                0,
                Instant.now(),
                null,
                null,
                null,
                null
        );
    }
}
