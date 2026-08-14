package com.techatlas.repository;

import java.time.LocalDateTime;

public interface ZeroResultProjection {
    String getQuery();
    Long getCount();
    LocalDateTime getLastOccurrence();
}
