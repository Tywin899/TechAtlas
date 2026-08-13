package com.techatlas.fetcher.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowAnswerItem(
    @JsonProperty("answer_id") Long answerId,
    @JsonProperty("body") String body,
    @JsonProperty("score") int score,
    @JsonProperty("is_accepted") boolean isAccepted,
    @JsonProperty("owner") StackOverflowOwner owner
) {}
