package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cache.redis")
public class RedisCacheProperties {
    private boolean enabled = true;
    private String host = "localhost";
    private int port = 6379;
    private final TtlConfig search = new TtlConfig(300);
    private final TtlConfig document = new TtlConfig(600);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public TtlConfig getSearch() {
        return search;
    }

    public TtlConfig getDocument() {
        return document;
    }

    public static class TtlConfig {
        private long ttlSeconds;

        public TtlConfig() {}

        public TtlConfig(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
