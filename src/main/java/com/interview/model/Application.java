package com.interview.model;

import com.interview.service.ApplicationStatus;

import java.util.UUID;

public record Application(
        UUID id,
        UUID clientId,
        ApplicationStatus status,
        String kycSessionId
) {
}
