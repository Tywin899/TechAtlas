package com.techatlas.sync.adapter;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.github.GitHubClient;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.mapper.GitHubMapper;
import com.techatlas.sync.SourceResource;
import com.techatlas.sync.SourceSynchronizer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class GitHubSynchronizer implements SourceSynchronizer {

    private final GitHubClient githubClient;
    private final GitHubMapper githubMapper;

    public GitHubSynchronizer(GitHubClient githubClient, GitHubMapper githubMapper) {
        this.githubClient = githubClient;
        this.githubMapper = githubMapper;
    }

    @Override
    public SourceType getSource() {
        return SourceType.GITHUB;
    }

    @Override
    public SourceResource fetchResource(String externalId, String originalTitle) throws Exception {
        String fullName = originalTitle.trim();
        String[] parts = fullName.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid GitHub repository full name format: " + fullName);
        }
        String owner = parts[0];
        String repo = parts[1];

        GitHubRepoItem item = githubClient.fetchRepository(owner, repo);
        if (item == null) {
            return null;
        }

        String readmeContent = null;
        String readmeSha = null;
        GitHubReadmeResponse readmeResponse = githubClient.fetchReadme(owner, repo);
        if (readmeResponse != null) {
            readmeSha = readmeResponse.sha();
            if (readmeResponse.content() != null && "base64".equalsIgnoreCase(readmeResponse.encoding())) {
                byte[] decodedBytes = Base64.getMimeDecoder().decode(readmeResponse.content().replaceAll("\\s", ""));
                readmeContent = new String(decodedBytes, StandardCharsets.UTF_8);
            }
        }

        CreateDocumentRequest createRequest = githubMapper.toCreateRequest(item, readmeContent, null);

        return new SourceResource(
                SourceType.GITHUB,
                item.id() != null ? item.id().toString() : externalId,
                readmeSha,
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
