package com.interview.service;

import com.interview.kyc.KycClient;
import com.interview.kyc.KycResult;
import com.interview.model.Application;
import com.interview.model.NotificationTask;
import com.interview.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationServiceJavaTest {

    private final UUID applicationId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private final Application pending = new Application(
            applicationId, clientId,
            ApplicationStatus.KYC_PENDING, "session-1"
    );


    private ApplicationRepository repositoryReturning(Application application) {
        return new ApplicationRepository() {
            @Override
            public Application findById(UUID id) {
                return application;
            }

            @Override
            public Application update(Application updated) {
                throw new UnsupportedOperationException(
                        "completeKyc не должен вызывать update"
                );
            }
        };
    }

    private KycResult kycResult(boolean completed) {
        return new KycResult(completed, "test");
    }

    private Application applicationWith(ApplicationStatus status, String sessionId) {
        return new Application(applicationId, clientId, status, sessionId);
    }

    private ApplicationServiceJava serviceWith(
            Application application,
            KycClient kycClient,
            AtomicInteger saveCalls
    ) {
        return new ApplicationServiceJava(
                repositoryReturning(application),
                kycClient,
                (app, task) -> {
                    saveCalls.incrementAndGet();
                    return app;
                }
        );
    }

    @Test
    void completesKycAndSavesTask() {
        AtomicReference<NotificationTask> savedTask = new AtomicReference<>();

        ApplicationServiceJava service = new ApplicationServiceJava(
                repositoryReturning(pending),
                sessionId -> kycResult(true),
                (application, task) -> {
                    savedTask.set(task);
                    return application;
                }
        );

        Application result = service.completeKyc(applicationId, clientId);

        assertEquals(ApplicationStatus.KYC_COMPLETED, result.status());
        assertNotNull(savedTask.get());
        assertEquals(applicationId, savedTask.get().applicationId());
        assertEquals(clientId, savedTask.get().clientId());
    }

    @Test
    void doesNotSaveWhenKycNotCompleted() {
        AtomicInteger saveCalls = new AtomicInteger();
        ApplicationServiceJava service =
                serviceWith(pending, s -> kycResult(false), saveCalls);

        assertThrows(IllegalStateException.class,
                () -> service.completeKyc(applicationId, clientId));
        assertEquals(0, saveCalls.get());
    }

    @Test
    void rejectsNullIds() {
        ApplicationServiceJava service =
                serviceWith(pending, s -> kycResult(true), new AtomicInteger());

        assertThrows(IllegalArgumentException.class,
                () -> service.completeKyc(null, clientId));
        assertThrows(IllegalArgumentException.class,
                () -> service.completeKyc(applicationId, null));
    }

    @Test
    void rejectsApplicationNotFound() {
        AtomicInteger saveCalls = new AtomicInteger();
        ApplicationServiceJava service =
                serviceWith(null, s -> kycResult(true), saveCalls);

        assertThrows(IllegalArgumentException.class,
                () -> service.completeKyc(applicationId, clientId));
        assertEquals(0, saveCalls.get());
    }

    @Test
    void rejectsForeignClient() {
        AtomicInteger kycCalls = new AtomicInteger();
        AtomicInteger saveCalls = new AtomicInteger();
        ApplicationServiceJava service = serviceWith(pending,
                s -> { kycCalls.incrementAndGet(); return kycResult(true); },
                saveCalls);

        assertThrows(IllegalArgumentException.class,
                () -> service.completeKyc(applicationId, UUID.randomUUID()));
        assertEquals(0, kycCalls.get());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void rejectsWrongStatus() {
        AtomicInteger kycCalls = new AtomicInteger();
        AtomicInteger saveCalls = new AtomicInteger();
        ApplicationServiceJava service = serviceWith(
                applicationWith(ApplicationStatus.KYC_COMPLETED, "session-1"),
                s -> { kycCalls.incrementAndGet(); return kycResult(true); },
                saveCalls);

        assertThrows(IllegalStateException.class,
                () -> service.completeKyc(applicationId, clientId));
        assertEquals(0, kycCalls.get());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void rejectsNullOrBlankKycSessionId() {
        for (String sessionId : new String[]{null, "", "   "}) {
            AtomicInteger kycCalls = new AtomicInteger();
            AtomicInteger saveCalls = new AtomicInteger();
            ApplicationServiceJava service = serviceWith(
                    applicationWith(ApplicationStatus.KYC_PENDING, sessionId),
                    s -> { kycCalls.incrementAndGet(); return kycResult(true); },
                    saveCalls);

            assertThrows(IllegalStateException.class,
                    () -> service.completeKyc(applicationId, clientId));
            assertEquals(0, kycCalls.get());
            assertEquals(0, saveCalls.get());
        }
    }

    @Test
    void wrapsKycClientFailure() {
        AtomicInteger saveCalls = new AtomicInteger();
        RuntimeException failure = new RuntimeException("boom");
        ApplicationServiceJava service = serviceWith(pending,
                s -> { throw failure; }, saveCalls);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.completeKyc(applicationId, clientId));
        assertSame(failure, thrown.getCause());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void rejectsNullKycResult() {
        AtomicInteger saveCalls = new AtomicInteger();
        ApplicationServiceJava service =
                serviceWith(pending, s -> null, saveCalls);

        assertThrows(IllegalStateException.class,
                () -> service.completeKyc(applicationId, clientId));
        assertEquals(0, saveCalls.get());
    }
}