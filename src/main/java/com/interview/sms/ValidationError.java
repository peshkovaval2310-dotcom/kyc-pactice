package com.interview.sms;

public record ValidationError(
        String reason
) implements NotificationResult {
}
