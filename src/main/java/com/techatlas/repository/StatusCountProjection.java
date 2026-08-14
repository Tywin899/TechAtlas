package com.techatlas.repository;

import com.techatlas.entity.DocumentStatus;

public interface StatusCountProjection {
    DocumentStatus getStatus();
    Long getCount();
}
