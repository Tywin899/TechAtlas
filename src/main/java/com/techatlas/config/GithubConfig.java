package com.techatlas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubConfig {

    private final GithubProperties githubProperties;

    public GithubConfig(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    @Bean
    public RestClient githubRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(githubProperties.getTimeout());
        requestFactory.setReadTimeout(githubProperties.getTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubProperties.getApiUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "TechAtlas/1.0 (support@techatlas.com)")
                .defaultHeader("Accept", "application/vnd.github.v3+json");

        String token = githubProperties.getToken();
        if (token != null && !token.trim().isEmpty()) {
            builder.defaultHeader("Authorization", "token " + token.trim());
        }

        return builder.build();
    }
}
