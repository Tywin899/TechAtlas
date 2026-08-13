package com.techatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    private Bm25 bm25 = new Bm25();
    private Pagination pagination = new Pagination();

    public Bm25 getBm25() {
        return bm25;
    }

    public void setBm25(Bm25 bm25) {
        this.bm25 = bm25;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Bm25 {
        private double k1 = 1.2;
        private double b = 0.75;

        public double getK1() {
            return k1;
        }

        public void setK1(double k1) {
            this.k1 = k1;
        }

        public double getB() {
            return b;
        }

        public void setB(double b) {
            this.b = b;
        }
    }

    public static class Pagination {
        private int defaultSize = 10;
        private int maxSize = 100;

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}
