package com.techatlas.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PostingList {
    private final List<Posting> postings;

    public PostingList() {
        this.postings = new CopyOnWriteArrayList<>();
    }

    public PostingList(List<Posting> postings) {
        this.postings = new CopyOnWriteArrayList<>(postings);
    }

    public List<Posting> getPostings() {
        return Collections.unmodifiableList(postings);
    }

    public void addPosting(Posting posting) {
        this.postings.add(posting);
    }

    public void removePostingForDocument(UUID documentId) {
        postings.removeIf(posting -> posting.documentId().equals(documentId));
    }
}
