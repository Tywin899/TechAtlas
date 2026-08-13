package com.techatlas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class StackOverflowConfig {

    private final StackOverflowProperties properties;

    public StackOverflowConfig(StackOverflowProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient stackOverflowRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());

        return RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "TechAtlas/1.0 (support@techatlas.com)")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
