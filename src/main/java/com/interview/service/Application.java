package com.interview.service;

import java.util.UUID;

public record Application(
        UUID id,
        UUID clientId,
        ApplicationStatus status,
        String kycSessionId
) {
}
