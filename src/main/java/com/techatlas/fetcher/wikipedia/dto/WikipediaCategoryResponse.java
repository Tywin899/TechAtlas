package com.techatlas.fetcher.wikipedia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WikipediaCategoryResponse(
    @JsonProperty("continue") WikipediaContinue continueToken,
    @JsonProperty("query") WikipediaQuery query
) {
    public record WikipediaContinue(
        @JsonProperty("cmcontinue") String cmcontinue,
        @JsonProperty("continue") String continueParam
    ) {}

    public record WikipediaQuery(
        @JsonProperty("categorymembers") List<WikipediaCategoryMember> categorymembers
    ) {}

    public record WikipediaCategoryMember(
        @JsonProperty("pageid") long pageId,
        @JsonProperty("ns") int ns,
        @JsonProperty("title") String title
    ) {}
}
