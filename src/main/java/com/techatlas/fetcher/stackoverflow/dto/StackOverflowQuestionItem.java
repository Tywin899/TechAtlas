package com.techatlas.fetcher.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowQuestionItem(
    @JsonProperty("question_id") Long questionId,
    @JsonProperty("title") String title,
    @JsonProperty("body") String body,
    @JsonProperty("link") String link,
    @JsonProperty("score") int score,
    @JsonProperty("tags") List<String> tags,
    @JsonProperty("owner") StackOverflowOwner owner,
    @JsonProperty("is_answered") boolean isAnswered,
    @JsonProperty("answer_count") int answerCount,
    @JsonProperty("accepted_answer_id") Long acceptedAnswerId,
    @JsonProperty("last_activity_date") Long lastActivityDate
) {}
