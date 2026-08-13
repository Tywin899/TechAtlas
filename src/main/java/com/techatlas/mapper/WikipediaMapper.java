package com.techatlas.mapper;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WikipediaMapper {

    private final ObjectMapper objectMapper;

    public WikipediaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CreateDocumentRequest toCreateRequest(WikipediaPageSummary summary, String category) {
        if (summary == null) {
            return null;
        }

        String desktopUrl = "";
        if (summary.contentUrls() != null && summary.contentUrls().desktop() != null) {
            desktopUrl = summary.contentUrls().desktop().page();
        }

        String metadataJson;
        try {
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("pageId", summary.pageId());
            metadataMap.put("description", summary.description());
            metadataMap.put("revision", summary.revision());
            if (summary.thumbnail() != null) {
                metadataMap.put("thumbnailUrl", summary.thumbnail().source());
            }
            metadataJson = objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        return new CreateDocumentRequest(
                summary.title(),
                summary.extract(),
                desktopUrl,
                SourceType.WIKIPEDIA,
                category,
                "Wikipedia", // author
                summary.lang(),
                metadataJson
        );
    }

    public CreateDocumentRequest toCreateRequest(WikipediaPageSummary summary) {
        return toCreateRequest(summary, null);
    }
}
