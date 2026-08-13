package com.techatlas.fetcher.stackoverflow;

import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;

import java.util.List;

public interface StackOverflowClient {
    StackOverflowResponseWrapper<StackOverflowQuestionItem> searchQuestions(String query, List<String> tags, int page, int pageSize);
    StackOverflowResponseWrapper<StackOverflowAnswerItem> fetchAnswers(Long questionId);
    StackOverflowResponseWrapper<StackOverflowQuestionItem> fetchQuestion(Long questionId);
}
