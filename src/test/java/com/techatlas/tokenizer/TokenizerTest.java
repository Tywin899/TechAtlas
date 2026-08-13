package com.techatlas.tokenizer;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerTest {

    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    public void testTokenizeSplitsOnWhitespaceAndRemovesPunctuation() {
        String text = "Spring Boot simplifies Java development.";
        List<String> tokens = tokenizer.tokenize(text);
        assertThat(tokens).containsExactly("Spring", "Boot", "simplifies", "Java", "development");
    }

    @Test
    public void testTokenizePreservesAlphanumericAndUnicode() {
        String text = "Java21 JDK 21 is fast Unicode Ä ö and ";
        List<String> tokens = tokenizer.tokenize(text);
        assertThat(tokens).containsExactly("Java21", "JDK", "21", "is", "fast", "Unicode", "Ä", "ö", "and");
    }

    @Test
    public void testTokenizeEmptyOrNull() {
        assertThat(tokenizer.tokenize(null)).isEmpty();
        assertThat(tokenizer.tokenize("   ")).isEmpty();
    }
}
