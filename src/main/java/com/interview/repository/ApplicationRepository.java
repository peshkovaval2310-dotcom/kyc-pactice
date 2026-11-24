package com.interview.repository;

import com.interview.service.Application;

import java.util.UUID;

public interface ApplicationRepository {
    Application findById(UUID id);
    Application update(Application application);


}
