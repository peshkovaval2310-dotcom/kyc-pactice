package com.interview.sms;



public sealed interface NotificationResult
        permits Success, ValidationError {
}
