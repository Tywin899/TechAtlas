package com.techatlas.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.util.HtmlToTextParser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StackOverflowMapper {

    private final ObjectMapper objectMapper;

    public StackOverflowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CreateDocumentRequest toCreateRequest(
            StackOverflowQuestionItem question,
            List<StackOverflowAnswerItem> answers,
            String query) {
        if (question == null) {
            return null;
        }

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("# ").append(question.title()).append("\n\n");
        if (question.body() != null && !question.body().isEmpty()) {
            contentBuilder.append(HtmlToTextParser.clean(question.body())).append("\n\n");
        }

        if (answers != null && !answers.isEmpty()) {
            contentBuilder.append("## Answers\n\n");
            for (StackOverflowAnswerItem answer : answers) {
                String author = "Anonymous";
                if (answer.owner() != null && answer.owner().displayName() != null) {
                    author = answer.owner().displayName();
                }
                contentBuilder.append("### Answer by ").append(author)
                        .append(" (Score: ").append(answer.score());
                if (answer.isAccepted()) {
                    contentBuilder.append(", Accepted");
                }
                contentBuilder.append(")\n");
                
                if (answer.body() != null && !answer.body().isEmpty()) {
                    contentBuilder.append(HtmlToTextParser.clean(answer.body())).append("\n\n");
                }
            }
        }

        String content = contentBuilder.toString();

        String metadataJson;
        try {
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("questionId", question.questionId());
            metadataMap.put("tags", question.tags());
            metadataMap.put("score", question.score());
            metadataMap.put("acceptedAnswerId", question.acceptedAnswerId());
            metadataMap.put("answerCount", question.answerCount());
            metadataMap.put("link", question.link());
            
            metadataJson = objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        String author = "Stack Overflow";
        if (question.owner() != null && question.owner().displayName() != null) {
            author = question.owner().displayName();
        }

        return new CreateDocumentRequest(
                question.title(),
                content,
                question.link(),
                SourceType.STACKOVERFLOW,
                query,
                author,
                "en",
                metadataJson
        );
    }
}
