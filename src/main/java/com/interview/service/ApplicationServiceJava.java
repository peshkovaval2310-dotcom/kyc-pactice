package com.interview.service;

import com.interview.kyc.KycClient;
import com.interview.kyc.KycResult;
import com.interview.model.Application;
import com.interview.service.ApplicationStatus;
import com.interview.model.NotificationTask;
import com.interview.repository.ApplicationRepository;
import com.interview.repository.KycCompletionRepository;


import java.util.UUID;


public class ApplicationServiceJava {

    private final ApplicationRepository applicationRepository;
    private final KycClient kycClient;
    private final KycCompletionRepository kycCompletionRepository;


    public ApplicationServiceJava(ApplicationRepository applicationRepository, KycClient kycClient, KycCompletionRepository kycCompletionRepository) {
        this.applicationRepository = applicationRepository;
        this.kycClient = kycClient;
        this.kycCompletionRepository = kycCompletionRepository;
    }

    public Application completeKyc(UUID applicationId, UUID clientId ) {
        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "Application ID must not be null"
            );
        }
        if (clientId == null) {
            throw new IllegalArgumentException(
                    "Client ID must not be null"
            );
        }
        Application application =
                applicationRepository.findById(applicationId);

        if (application == null) {
            throw new IllegalArgumentException(
                    "Application not found: " + applicationId
            );
        }
        if (!application.clientId().equals(clientId)) {
            throw new IllegalArgumentException(
                    "Application does not belong to client: " + clientId

            );
        }


        if (application.status() != ApplicationStatus.KYC_PENDING) {
            throw new IllegalStateException(
                    "Application must have KYC_PENDING status, actual: "
                            + application.status()
            );
        }
        String kycSessionId = application.kycSessionId();
        if (kycSessionId == null || kycSessionId.isBlank()) {
            throw new IllegalStateException(
                    "Application has no KYC session ID"
            );
        }
        KycResult kycResult;
        try {
            kycResult = kycClient.fetchStatus(kycSessionId);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "KYC service call failed for application: "
                            + applicationId,
                    exception
            );
        }

        if (kycResult == null) {
            throw new IllegalStateException(
                    "KYC service returned no result"
            );
        }
        if (!kycResult.completed()) {
            throw new IllegalStateException(
                    "KYC is not completed for application: " + applicationId
            );
        }

        Application completedApplication = new Application(
                application.id(),
                application.clientId(),
                ApplicationStatus.KYC_COMPLETED,
                application.kycSessionId()
        );
        NotificationTask notificationTask =
                NotificationTask.pendingKycCompleteSms(completedApplication);

        return kycCompletionRepository.completeWithNotification(
                completedApplication,
                notificationTask
        );

    }


    }

