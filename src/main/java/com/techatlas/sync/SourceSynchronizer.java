package com.techatlas.sync;

import com.techatlas.entity.SourceType;

public interface SourceSynchronizer {
    SourceType getSource();
    SourceResource fetchResource(String externalId, String originalTitle) throws Exception;
}
