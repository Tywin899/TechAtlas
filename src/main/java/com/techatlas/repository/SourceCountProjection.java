package com.techatlas.repository;

import com.techatlas.entity.SourceType;

public interface SourceCountProjection {
    SourceType getSource();
    Long getCount();
}
