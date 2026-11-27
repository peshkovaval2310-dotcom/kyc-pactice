CREATE TABLE public.notification_tasks (
                                           id UUID PRIMARY KEY,
                                           application_id UUID NOT NULL,
                                           client_id UUID NOT NULL,
                                           text TEXT NOT NULL,
                                           status TEXT NOT NULL,
                                           attempts INTEGER NOT NULL DEFAULT 0,
                                           next_attempt_at TIMESTAMPTZ NOT NULL,
                                           last_error TEXT,
                                           locked_until TIMESTAMPTZ,
                                           claim_token UUID,
                                           sent_at TIMESTAMPTZ,

                                           CONSTRAINT uq_notification_tasks_application
                                               UNIQUE (application_id),

                                           CONSTRAINT fk_notification_tasks_application
                                               FOREIGN KEY (application_id)
                                                   REFERENCES public.applications (id),

                                           CONSTRAINT chk_notification_tasks_attempts
                                               CHECK (attempts >= 0),

                                           CONSTRAINT chk_notification_tasks_status
                                               CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'))
);