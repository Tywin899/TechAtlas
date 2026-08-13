package com.techatlas.util;

import org.springframework.web.util.HtmlUtils;

public class HtmlToTextParser {

    private HtmlToTextParser() {}

    public static String clean(String html) {
        if (html == null) {
            return "";
        }

        String text = html;

        // 1. Convert <pre><code>...</code></pre> to markdown code blocks
        text = text.replaceAll("(?i)<pre>\\s*<code>", "\n\n```\n");
        text = text.replaceAll("(?i)</code>\\s*</pre>", "\n```\n\n");

        // 2. Convert <code>...</code> to inline backticks
        text = text.replaceAll("(?i)<code>", "`");
        text = text.replaceAll("(?i)</code>", "`");

        // 3. Strip all other HTML tags
        text = text.replaceAll("<[^>]*>", "");

        // 4. Unescape HTML entities
        text = HtmlUtils.htmlUnescape(text);

        // 5. Normalize whitespace and collapse consecutive newlines
        text = text.replaceAll("(?m)^[ \t]*\r?\n", "\n");
        text = text.replaceAll("(\r?\n){3,}", "\n\n");

        return text.trim();
    }
}
