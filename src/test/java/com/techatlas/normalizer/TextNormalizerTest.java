package com.techatlas.normalizer;

import com.techatlas.config.IndexProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TextNormalizerTest {

    @Test
    public void testNormalizeEnabled() {
        IndexProperties properties = new IndexProperties();
        properties.setNormalization(true);
        TextNormalizer normalizer = new TextNormalizer(properties);

        assertThat(normalizer.normalize("  Spring   BOOT!  ")).isEqualTo("spring boot");
        assertThat(normalizer.normalize("Number 12345 preserved.")).isEqualTo("number 12345 preserved");
        assertThat(normalizer.normalize(null)).isEqualTo("");
    }

    @Test
    public void testNormalizeDisabled() {
        IndexProperties properties = new IndexProperties();
        properties.setNormalization(false);
        TextNormalizer normalizer = new TextNormalizer(properties);

        assertThat(normalizer.normalize("  Spring   BOOT!  ")).isEqualTo("  Spring   BOOT!  ");
    }
}
