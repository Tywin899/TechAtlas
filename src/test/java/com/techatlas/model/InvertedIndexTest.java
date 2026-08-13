package com.techatlas.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class InvertedIndexTest {

    @Test
    public void testInvertedIndexOperations() {
        InvertedIndex index = new InvertedIndex();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        index.insert("java", new Posting(docA, 2));
        index.insert("java", new Posting(docB, 1));
        index.insert("spring", new Posting(docA, 1));
        index.setDocumentLength(docA, 3);
        index.setDocumentLength(docB, 2);

        assertThat(index.getDocumentCount()).isEqualTo(2);
        assertThat(index.getVocabularySize()).isEqualTo(2);
        assertThat(index.getDocumentLength(docA)).isEqualTo(3);
        assertThat(index.getAverageDocumentLength()).isEqualTo(2.5);

        PostingList javaPostings = index.retrieve("java");
        assertThat(javaPostings).isNotNull();
        assertThat(javaPostings.getPostings()).hasSize(2);

        index.removeDocument(docA);
        assertThat(index.getDocumentCount()).isEqualTo(1);
        assertThat(index.getVocabularySize()).isEqualTo(1);
        assertThat(index.retrieve("spring")).isNull();
        assertThat(index.retrieve("java").getPostings()).hasSize(1);
        assertThat(index.getDocumentLength(docA)).isEqualTo(0);
        assertThat(index.getAverageDocumentLength()).isEqualTo(2.0);

        index.clear();
        assertThat(index.getDocumentCount()).isEqualTo(0);
        assertThat(index.getVocabularySize()).isEqualTo(0);
        assertThat(index.getAverageDocumentLength()).isEqualTo(0.0);
    }

    @Test
    public void testRemoveNonexistentDocument() {
        InvertedIndex index = new InvertedIndex();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        index.insert("java", new Posting(docA, 1));
        index.setDocumentLength(docA, 10);

        // Remove nonexistent document
        index.removeDocument(docB);

        assertThat(index.getDocumentCount()).isEqualTo(1);
        assertThat(index.getVocabularySize()).isEqualTo(1);
        assertThat(index.getDocumentLength(docA)).isEqualTo(10);
        assertThat(index.getAverageDocumentLength()).isEqualTo(10.0);
    }

    @Test
    public void testReindexingUpdatesStatistics() {
        InvertedIndex index = new InvertedIndex();
        UUID docA = UUID.randomUUID();

        index.insert("java", new Posting(docA, 1));
        index.setDocumentLength(docA, 1);

        assertThat(index.getDocumentCount()).isEqualTo(1);
        assertThat(index.getVocabularySize()).isEqualTo(1);
        assertThat(index.getAverageDocumentLength()).isEqualTo(1.0);

        // Reindex docA with different terms
        index.removeDocument(docA);
        index.insert("python", new Posting(docA, 2));
        index.setDocumentLength(docA, 2);

        assertThat(index.getDocumentCount()).isEqualTo(1);
        assertThat(index.getVocabularySize()).isEqualTo(1);
        assertThat(index.retrieve("java")).isNull();
        assertThat(index.retrieve("python")).isNotNull();
        assertThat(index.getAverageDocumentLength()).isEqualTo(2.0);
    }
}
