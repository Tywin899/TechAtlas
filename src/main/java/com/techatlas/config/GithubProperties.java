package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github")
public class GithubProperties {
    private String apiUrl = "https://api.github.com";
    private String token;
    private int timeout = 5000;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
