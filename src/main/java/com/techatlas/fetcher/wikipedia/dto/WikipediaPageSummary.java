package com.techatlas.fetcher.wikipedia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WikipediaPageSummary(
    String title,
    String extract,
    String description,
    @JsonProperty("pageid") Long pageId,
    String lang,
    String revision,
    @JsonProperty("content_urls") ContentUrls contentUrls,
    Thumbnail thumbnail
) {
    public record ContentUrls(
        Desktop desktop,
        Mobile mobile
    ) {
        public record Desktop(
            String page
        ) {}
        public record Mobile(
            String page
        ) {}
    }

    public record Thumbnail(
        String source,
        int width,
        int height
    ) {}
}
