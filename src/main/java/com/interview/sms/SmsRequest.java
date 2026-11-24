package com.interview.sms;

public record SmsRequest(
        String clientId,
        String text
) {
}
