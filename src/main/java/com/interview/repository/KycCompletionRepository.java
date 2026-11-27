package com.interview.repository;

import com.interview.model.Application;
import com.interview.model.NotificationTask;

public interface KycCompletionRepository {

    Application completeWithNotification(
            Application application,
            NotificationTask notificationTask
    );
}