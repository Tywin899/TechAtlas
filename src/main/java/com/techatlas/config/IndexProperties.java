package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "index")
public class IndexProperties {
    private List<String> stopwords;
    private boolean stemming;
    private boolean normalization;

    public List<String> getStopwords() {
        return stopwords;
    }

    public void setStopwords(List<String> stopwords) {
        this.stopwords = stopwords;
    }

    public boolean isStemming() {
        return stemming;
    }

    public void setStemming(boolean stemming) {
        this.stemming = stemming;
    }

    public boolean isNormalization() {
        return normalization;
    }

    public void setNormalization(boolean normalization) {
        this.normalization = normalization;
    }
}
