package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stackoverflow")
public class StackOverflowProperties {
    private String apiUrl = "https://api.stackexchange.com/2.3";
    private String site = "stackoverflow";
    private int timeout = 5000;
    private int defaultPageSize = 30;
    private int maxPageSize = 100;
    private int maxAnswersPerQuestion = 1;
    private String apiKey;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getMaxAnswersPerQuestion() {
        return maxAnswersPerQuestion;
    }

    public void setMaxAnswersPerQuestion(int maxAnswersPerQuestion) {
        this.maxAnswersPerQuestion = maxAnswersPerQuestion;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
