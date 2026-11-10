package it.uniroma3.hw3.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    @DisplayName("TargetIndex: mapping robusto da stringhe utente")
    void testTargetIndexParsing() {
        assertEquals(TargetIndex.FILENAME, TargetIndex.fromString("nome"));
        assertEquals(TargetIndex.FILENAME, TargetIndex.fromString("FILENAME"));
        assertEquals(TargetIndex.FILENAME, TargetIndex.fromString("file"));
        assertEquals(TargetIndex.CONTENT, TargetIndex.fromString("contenuto"));
        assertEquals(TargetIndex.CONTENT, TargetIndex.fromString("CONTENT"));

        assertThrows(IllegalArgumentException.class, () -> TargetIndex.fromString("invalido"));
        assertThrows(NullPointerException.class, () -> TargetIndex.fromString(null));
    }

    @Test
    @DisplayName("SearchHit: validazione compatta e formattazione Information Expert")
    void testSearchHitValidationAndFormatting() {
        SearchHit hit = new SearchHit(
                42,
                1.5432f,
                "relazione.txt",
                "docs/relazione.txt",
                "/abs/path/docs/relazione.txt",
                2048L,
                "anteprima snippet"
        );

        assertEquals("2.00 KB", hit.getFormattedFileSize());
        assertTrue(hit.toConsoleRow(1).contains("#1"));
        assertTrue(hit.toConsoleRow(1).contains("1.5432"));
        assertTrue(hit.toConsoleRow(1).contains("relazione.txt"));

        // Validazione precondizioni
        assertThrows(IllegalArgumentException.class, () ->
                new SearchHit(-1, 1.0f, "a", "b", "c", 10L, null));
        assertThrows(IllegalArgumentException.class, () ->
                new SearchHit(1, -0.5f, "a", "b", "c", 10L, null));
        assertThrows(NullPointerException.class, () ->
                new SearchHit(1, 1.0f, null, "b", "c", 10L, null));
    }

    @Test
    @DisplayName("SearchResult: immutabilità e copia difensiva della lista hits")
    void testSearchResultImmutability() {
        List<SearchHit> mutableList = new ArrayList<>();
        mutableList.add(new SearchHit(1, 2.0f, "file1.txt", "file1.txt", "/file1.txt", 100L, null));

        SearchResult result = new SearchResult("database", TargetIndex.CONTENT, 1, 15, mutableList);
        assertTrue(result.hasHits());
        assertEquals(1, result.getHitsCount());

        // La modifica della lista sorgente non deve alterare l'oggetto SearchResult
        mutableList.clear();
        assertEquals(1, result.getHitsCount());

        // La lista interna deve essere non modificabile
        assertThrows(UnsupportedOperationException.class, () ->
                result.hits().add(new SearchHit(2, 1.0f, "file2.txt", "file2.txt", "/file2.txt", 50L, null)));
    }

    @Test
    @DisplayName("IndexMetrics: calcolo del throughput e compression ratio (Information Expert)")
    void testIndexMetricsCalculation() {
        // 1000 documenti in 2000 ms, corpus da 2 MB, indice su disco da 500 KB
        long corpusBytes = 2 * 1024 * 1024;
        long diskBytes = 512 * 1024;
        IndexMetrics metrics = new IndexMetrics(
                TargetIndex.CONTENT,
                1000,
                2000L,
                diskBytes,
                corpusBytes
        );

        // Throughput = 1000 doc / 2s = 500 doc/s
        assertEquals(500.0, metrics.getThroughputDocsPerSec(), 0.001);

        // Throughput MB/s = 2 MB / 2s = 1.0 MB/s
        assertEquals(1.0, metrics.getThroughputMBPerSec(), 0.001);

        // Compression ratio = 512 KB / 2048 KB = 0.25
        assertEquals(0.25, metrics.getCompressionRatio(), 0.001);

        assertEquals("512.00 KB", metrics.getFormattedDiskSize());
        assertEquals("2.00 MB", metrics.getFormattedCorpusSize());
    }
}
