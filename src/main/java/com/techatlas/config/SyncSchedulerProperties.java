package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sync.scheduler")
public class SyncSchedulerProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 3600000;
    private long initialDelayMs = 30000;

    private final SourceConfig wikipedia = new SourceConfig();
    private final SourceConfig github = new SourceConfig();
    private final SourceConfig stackoverflow = new SourceConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public SourceConfig getWikipedia() {
        return wikipedia;
    }

    public SourceConfig getGithub() {
        return github;
    }

    public SourceConfig getStackoverflow() {
        return stackoverflow;
    }

    public static class SourceConfig {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
