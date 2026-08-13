package com.techatlas.sync.adapter;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.wikipedia.WikipediaClient;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.mapper.WikipediaMapper;
import com.techatlas.sync.SourceResource;
import com.techatlas.sync.SourceSynchronizer;
import org.springframework.stereotype.Component;

@Component
public class WikipediaSynchronizer implements SourceSynchronizer {

    private final WikipediaClient wikipediaClient;
    private final WikipediaMapper wikipediaMapper;

    public WikipediaSynchronizer(WikipediaClient wikipediaClient, WikipediaMapper wikipediaMapper) {
        this.wikipediaClient = wikipediaClient;
        this.wikipediaMapper = wikipediaMapper;
    }

    @Override
    public SourceType getSource() {
        return SourceType.WIKIPEDIA;
    }

    @Override
    public SourceResource fetchResource(String externalId, String originalTitle) throws Exception {
        WikipediaPageSummary summary = wikipediaClient.fetchPageSummary(originalTitle);
        if (summary == null) {
            return null;
        }

        CreateDocumentRequest createRequest = wikipediaMapper.toCreateRequest(summary);

        return new SourceResource(
                SourceType.WIKIPEDIA,
                summary.pageId() != null ? summary.pageId().toString() : externalId,
                summary.revision(),
                createRequest.title(),
                createRequest.content(),
                createRequest.url(),
                createRequest.author(),
                createRequest.language(),
                createRequest.category(),
                createRequest.metadata()
        );
    }
}
