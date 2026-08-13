package com.techatlas.mapper;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GitHubMapper {

    private final ObjectMapper objectMapper;

    public GitHubMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CreateDocumentRequest toCreateRequest(GitHubRepoItem item, String readmeContent, String query) {
        if (item == null) {
            return null;
        }

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("# ").append(item.fullName()).append("\n\n");
        if (item.description() != null && !item.description().isEmpty()) {
            contentBuilder.append(item.description()).append("\n\n");
        }
        if (readmeContent != null && !readmeContent.isEmpty()) {
            contentBuilder.append(readmeContent);
        }
        String content = contentBuilder.toString();

        String metadataJson;
        try {
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("repositoryId", item.id());
            metadataMap.put("owner", item.owner() != null ? item.owner().login() : null);
            metadataMap.put("name", item.name());
            metadataMap.put("description", item.description());
            metadataMap.put("stars", item.stargazersCount());
            metadataMap.put("forks", item.forksCount());
            metadataMap.put("topics", item.topics());
            metadataMap.put("defaultBranch", item.defaultBranch());
            metadataMap.put("license", item.license() != null ? item.license().name() : null);
            
            metadataJson = objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }

        return new CreateDocumentRequest(
                item.fullName(),
                content,
                item.htmlUrl(),
                SourceType.GITHUB,
                query,
                item.owner() != null ? item.owner().login() : "GitHub",
                item.language(),
                metadataJson
        );
    }
}
