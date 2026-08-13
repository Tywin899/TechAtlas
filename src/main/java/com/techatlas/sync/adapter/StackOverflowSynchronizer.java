package com.techatlas.sync.adapter;

import com.techatlas.config.StackOverflowProperties;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.stackoverflow.StackOverflowClient;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import com.techatlas.mapper.StackOverflowMapper;
import com.techatlas.sync.SourceResource;
import com.techatlas.sync.SourceSynchronizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StackOverflowSynchronizer implements SourceSynchronizer {

    private final StackOverflowClient client;
    private final StackOverflowMapper mapper;
    private final StackOverflowProperties properties;

    public StackOverflowSynchronizer(
            StackOverflowClient client,
            StackOverflowMapper mapper,
            StackOverflowProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public SourceType getSource() {
        return SourceType.STACKOVERFLOW;
    }

    @Override
    public SourceResource fetchResource(String externalId, String originalTitle) throws Exception {
        Long questionId = Long.parseLong(externalId);
        StackOverflowResponseWrapper<StackOverflowQuestionItem> qResponse = client.fetchQuestion(questionId);
        if (qResponse == null || qResponse.items() == null || qResponse.items().isEmpty()) {
            return null;
        }

        StackOverflowQuestionItem item = qResponse.items().get(0);

        List<StackOverflowAnswerItem> sortedAnswers = new ArrayList<>();
        if (item.answerCount() > 0) {
            StackOverflowResponseWrapper<StackOverflowAnswerItem> answersResponse = client.fetchAnswers(questionId);
            if (answersResponse != null && answersResponse.items() != null) {
                List<StackOverflowAnswerItem> allAnswers = answersResponse.items();
                
                List<StackOverflowAnswerItem> accepted = allAnswers.stream()
                        .filter(StackOverflowAnswerItem::isAccepted)
                        .toList();
                
                List<StackOverflowAnswerItem> others = allAnswers.stream()
                        .filter(a -> !a.isAccepted())
                        .sorted((a1, a2) -> Integer.compare(a2.score(), a1.score()))
                        .toList();
                
                sortedAnswers.addAll(accepted);
                sortedAnswers.addAll(others);
                
                int maxAnswers = properties.getMaxAnswersPerQuestion();
                if (sortedAnswers.size() > maxAnswers) {
                    sortedAnswers = sortedAnswers.subList(0, maxAnswers);
                }
            }
        }

        CreateDocumentRequest createRequest = mapper.toCreateRequest(item, sortedAnswers, null);

        String revision = item.lastActivityDate() != null ? item.lastActivityDate().toString() : null;

        return new SourceResource(
                SourceType.STACKOVERFLOW,
                item.questionId() != null ? item.questionId().toString() : externalId,
                revision,
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
