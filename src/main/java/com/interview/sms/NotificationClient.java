package com.interview.sms;

public interface NotificationClient {
    NotificationResult sendSms(SmsRequest request);
}
