package com.techatlas.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HtmlToTextParserTest {

    @Test
    void testCleanHtmlBasic() {
        String html = "<p>Hello <strong>World</strong>!</p>";
        String cleaned = HtmlToTextParser.clean(html);
        assertEquals("Hello World!", cleaned);
    }

    @Test
    void testCleanHtmlWithEntities() {
        String html = "<p>Java &lt; 21 &amp;&amp; Kotlin &gt; 1.9</p>";
        String cleaned = HtmlToTextParser.clean(html);
        assertEquals("Java < 21 && Kotlin > 1.9", cleaned);
    }

    @Test
    void testCleanHtmlWithPreCode() {
        String html = "<p>Check this method:</p><pre><code>public void test() {\n    System.out.println(\"Hi\");\n}</code></pre>";
        String cleaned = HtmlToTextParser.clean(html);
        String expected = "Check this method:\n\n```\npublic void test() {\n    System.out.println(\"Hi\");\n}\n```";
        assertEquals(expected, cleaned);
    }

    @Test
    void testCleanHtmlWithInlineCode() {
        String html = "<p>Use the <code>var</code> keyword.</p>";
        String cleaned = HtmlToTextParser.clean(html);
        assertEquals("Use the `var` keyword.", cleaned);
    }

    @Test
    void testCleanHtmlNullOrEmpty() {
        assertEquals("", HtmlToTextParser.clean(null));
        assertEquals("", HtmlToTextParser.clean(""));
    }
}
