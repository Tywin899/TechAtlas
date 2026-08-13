package com.techatlas.stopwords;

import com.techatlas.config.IndexProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class StopWordFilterTest {

    @Test
    public void testStopWordFilterWithConfiguredList() {
        IndexProperties properties = new IndexProperties();
        properties.setStopwords(List.of("the", "and", "or"));
        StopWordFilter filter = new StopWordFilter(properties);

        assertThat(filter.isStopWord("the")).isTrue();
        assertThat(filter.isStopWord("AND")).isTrue();
        assertThat(filter.isStopWord("java")).isFalse();
        assertThat(filter.isStopWord(null)).isFalse();
    }

    @Test
    public void testStopWordFilterWithDefaultFallback() {
        StopWordFilter filter = new StopWordFilter(null);
        assertThat(filter.isStopWord("the")).isTrue();
        assertThat(filter.isStopWord("with")).isTrue();
        assertThat(filter.isStopWord("java")).isFalse();
    }
}
