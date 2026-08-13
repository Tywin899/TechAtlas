package com.techatlas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WikipediaConfig {

    @Value("${wikipedia.base-url}")
    private String baseUrl;

    @Value("${wikipedia.timeout}")
    private int timeout;

    @Bean
    public RestClient wikipediaRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "TechAtlas/1.0 (support@techatlas.com)")
                .build();
    }
}
