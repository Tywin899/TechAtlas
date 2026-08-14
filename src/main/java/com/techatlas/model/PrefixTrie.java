package com.techatlas.model;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PrefixTrie {

    private final TrieNode root = new TrieNode();

    public void insert(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        TrieNode current = root;
        for (char ch : term.toCharArray()) {
            current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());
        }
        current.setTerm(term);
    }

    public void remove(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        remove(root, term, 0);
    }

    private boolean remove(TrieNode current, String term, int index) {
        if (index == term.length()) {
            if (current.getTerm() == null) {
                return false;
            }
            current.setTerm(null);
            return current.getChildren().isEmpty();
        }
        char ch = term.charAt(index);
        TrieNode child = current.getChildren().get(ch);
        if (child == null) {
            return false;
        }
        boolean shouldDeleteChild = remove(child, term, index + 1);
        if (shouldDeleteChild) {
            current.getChildren().remove(ch);
            return current.getTerm() == null && current.getChildren().isEmpty();
        }
        return false;
    }

    public List<String> prefixLookup(String prefix) {
        if (prefix == null) {
            return Collections.emptyList();
        }
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            current = current.getChildren().get(ch);
            if (current == null) {
                return Collections.emptyList();
            }
        }
        List<String> results = new ArrayList<>();
        collectTerms(current, results);
        return results;
    }

    private void collectTerms(TrieNode node, List<String> results) {
        if (node.getTerm() != null) {
            results.add(node.getTerm());
        }
        for (TrieNode child : node.getChildren().values()) {
            collectTerms(child, results);
        }
    }

    public void clear() {
        root.getChildren().clear();
        root.setTerm(null);
    }
}
