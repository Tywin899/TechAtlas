package com.techatlas.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrieNode {
    private final Map<Character, TrieNode> children = new ConcurrentHashMap<>();
    private volatile String term = null;

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
