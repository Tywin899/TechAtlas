package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "autocomplete")
public class AutocompleteProperties {
    private boolean enabled = true;
    private int defaultLimit = 10;
    private int maxLimit = 20;
    private int maxPrefixLength = 50;

    private final SubConfig popularQuery = new SubConfig(1000);
    private final SubConfig recentQuery = new SubConfig(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public int getMaxPrefixLength() {
        return maxPrefixLength;
    }

    public void setMaxPrefixLength(int maxPrefixLength) {
        this.maxPrefixLength = maxPrefixLength;
    }

    public SubConfig getPopularQuery() {
        return popularQuery;
    }

    public SubConfig getRecentQuery() {
        return recentQuery;
    }

    public static class SubConfig {
        private boolean enabled = true;
        private int maxSize;

        public SubConfig() {}

        public SubConfig(int maxSize) {
            this.maxSize = maxSize;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}
