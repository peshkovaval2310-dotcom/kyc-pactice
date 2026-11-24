package com.interview.kyc;

public interface KycClient {

    KycResult fetchStatus(String kysSessionId);
}
