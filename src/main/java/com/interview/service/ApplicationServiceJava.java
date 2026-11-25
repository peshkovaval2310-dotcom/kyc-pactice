package com.interview.service;

import com.interview.kyc.KycClient;
import com.interview.kyc.KycResult;
import com.interview.repository.ApplicationRepository;
import com.interview.sms.NotificationClient;
import com.interview.sms.NotificationResult;
import com.interview.sms.SmsRequest;
import com.interview.sms.ValidationError;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ApplicationServiceJava {

    private final ApplicationRepository applicationRepository;
    private final KycClient kycClient;
    private final NotificationClient notificationClient;


    public ApplicationServiceJava(ApplicationRepository applicationRepository, KycClient kycClient, NotificationClient notificationClient) {
        this.applicationRepository = applicationRepository;
        this.kycClient = kycClient;
        this.notificationClient = notificationClient;
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
        KycResult kycResult = kycClient.fetchStatus(kycSessionId);
        if (kycResult == null) {
            throw new IllegalStateException(
                    "KYC service returned no result"
            );
        }

        if (!kycResult.completed()) {
            throw new IllegalStateException(
                    "KYC is not completed"
            );
        }

        Application completedApplication = new Application(
                application.id(),
                application.clientId(),
                ApplicationStatus.KYC_COMPLETED,
                application.kycSessionId()
        );
        Application updatedApplication =
                applicationRepository.update(completedApplication);
        if (updatedApplication == null) {
            throw new IllegalStateException(
                    "Application update returned no result"
            );
        }
        sendKycCompletedSmsAsync(updatedApplication)
                .exceptionally(error -> {
                    System.err.println(
                    "Failed to send KYC SMS for application "
                            + updatedApplication.id()
                            + ": " + error.getMessage()
            );

        return null;
                });

        return updatedApplication;
    }

    private CompletableFuture<Void> sendKycCompletedSmsAsync(Application application){
        return CompletableFuture.runAsync(() ->sendKycCompletedSms(application));
    }

    private void sendKycCompletedSms(Application application){
        SmsRequest smsRequest = new SmsRequest(
                application.clientId().toString(),
                "Проверка личности успешно завершена."
        );



        NotificationResult notificationResult =
                notificationClient.sendSms(smsRequest);

        if (notificationResult == null) {
            throw new IllegalStateException(
                    "Notification service returned no result"
            );
        }

        if (notificationResult instanceof ValidationError(String reason)) {
            throw new IllegalStateException(
                    "SMS sending failed: " + reason
            );
        }
    }
}
