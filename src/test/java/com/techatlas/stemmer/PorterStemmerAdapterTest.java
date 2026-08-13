package com.techatlas.stemmer;

import com.techatlas.config.IndexProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class PorterStemmerAdapterTest {

    @Test
    public void testStemmingEnabled() {
        IndexProperties properties = new IndexProperties();
        properties.setStemming(true);
        PorterStemmerAdapter adapter = new PorterStemmerAdapter(properties);

        assertThat(adapter.stem("running")).isEqualTo("run");
        assertThat(adapter.stem("developing")).isEqualTo("develop");
        assertThat(adapter.stem("connections")).isEqualTo("connect");
        assertThat(adapter.stem(null)).isEqualTo("");
    }

    @Test
    public void testStemmingDisabled() {
        IndexProperties properties = new IndexProperties();
        properties.setStemming(false);
        PorterStemmerAdapter adapter = new PorterStemmerAdapter(properties);

        assertThat(adapter.stem("running")).isEqualTo("running");
        assertThat(adapter.stem("developing")).isEqualTo("developing");
    }
}
