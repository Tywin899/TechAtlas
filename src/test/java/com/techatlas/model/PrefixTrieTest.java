package com.techatlas.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

public class PrefixTrieTest {

    private PrefixTrie prefixTrie;

    @BeforeEach
    public void setUp() {
        prefixTrie = new PrefixTrie();
    }

    @Test
    public void testInsertAndPrefixLookup() {
        prefixTrie.insert("spring");
        prefixTrie.insert("springboot");
        prefixTrie.insert("springmvc");
        prefixTrie.insert("java");

        List<String> sprMatches = prefixTrie.prefixLookup("spr");
        assertThat(sprMatches).containsExactlyInAnyOrder("spring", "springboot", "springmvc");

        List<String> jMatches = prefixTrie.prefixLookup("ja");
        assertThat(jMatches).containsExactly("java");

        List<String> missingMatches = prefixTrie.prefixLookup("missing");
        assertThat(missingMatches).isEmpty();
    }

    @Test
    public void testRemoveTerm() {
        prefixTrie.insert("spring");
        prefixTrie.insert("springboot");

        prefixTrie.remove("springboot");

        List<String> matches = prefixTrie.prefixLookup("spr");
        assertThat(matches).containsExactly("spring");
    }

    @Test
    public void testRemoveNonExistentTermIsSafe() {
        prefixTrie.insert("spring");
        prefixTrie.remove("nonexistent");
        List<String> matches = prefixTrie.prefixLookup("spr");
        assertThat(matches).containsExactly("spring");
    }

    @Test
    public void testClearTrie() {
        prefixTrie.insert("spring");
        prefixTrie.insert("java");

        prefixTrie.clear();

        assertThat(prefixTrie.prefixLookup("spr")).isEmpty();
        assertThat(prefixTrie.prefixLookup("ja")).isEmpty();
    }

    @Test
    public void testConcurrentInsertAndLookup() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> prefixTrie.insert("term" + index));
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        List<String> matches = prefixTrie.prefixLookup("term");
        assertThat(matches).hasSize(100);
    }
}
