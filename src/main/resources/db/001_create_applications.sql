CREATE TABLE public.applications (
                              id UUID PRIMARY KEY,
                              client_id UUID NOT NULL,
                              status TEXT NOT NULL,
                              kyc_session_id TEXT
);