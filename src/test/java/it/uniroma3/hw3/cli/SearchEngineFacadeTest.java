package it.uniroma3.hw3.cli;

import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test di Integrazione SearchEngineFacade (Fase 6)")
class SearchEngineFacadeTest {

    @TempDir
    Path tempDir;

    private Path corpusDir;
    private Path filenameIndexDir;
    private Path contentIndexDir;
    private Path benchmarkDataDir;
    private Path benchmarkIndexDir;
    private DefaultSearchEngineFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        corpusDir = tempDir.resolve("corpus");
        filenameIndexDir = tempDir.resolve("filename_index");
        contentIndexDir = tempDir.resolve("content_index");
        benchmarkDataDir = tempDir.resolve("benchmark_data");
        benchmarkIndexDir = tempDir.resolve("benchmark_index");

        Files.createDirectories(corpusDir);
        Files.createDirectories(filenameIndexDir);
        Files.createDirectories(contentIndexDir);
        Files.createDirectories(benchmarkDataDir);
        Files.createDirectories(benchmarkIndexDir);

        // Prepariamo 3 file di test:
        // Due file contengono sia "retrieval" che "information retrieval"
        Files.writeString(corpusDir.resolve("information_retrieval_intro.txt"),
                "Apache Lucene provides powerful full-text search and information retrieval data structures.");
        Files.writeString(corpusDir.resolve("database_systems_advanced.txt"),
                "Relational databases use B-Trees for structured query optimization and ACID transactions.");
        Files.writeString(corpusDir.resolve("machine_learning_ir.txt"),
                "Machine learning methods are applied to information retrieval for ranking models.");

        facade = new DefaultSearchEngineFacade(filenameIndexDir, contentIndexDir, benchmarkDataDir, benchmarkIndexDir);
    }

    @AfterEach
    void tearDown() {
        if (facade != null) {
            facade.close();
        }
    }

    @Test
    @DisplayName("Dovrebbe indicizzare il corpus e popolare entrambi gli indici")
    void shouldIndexCorpusSuccessfully() throws Exception {
        Map<TargetIndex, IndexMetrics> metrics = facade.indexCorpus(corpusDir);

        assertNotNull(metrics);
        assertEquals(2, metrics.size());

        IndexMetrics filenameMetrics = metrics.get(TargetIndex.FILENAME);
        IndexMetrics contentMetrics = metrics.get(TargetIndex.CONTENT);

        assertNotNull(filenameMetrics);
        assertNotNull(contentMetrics);

        assertEquals(3, filenameMetrics.documentCount());
        assertEquals(3, contentMetrics.documentCount());
        assertTrue(filenameMetrics.diskSizeBytes() > 0);
        assertTrue(contentMetrics.diskSizeBytes() > 0);
    }

    @Test
    @DisplayName("Dovrebbe eseguire ricerche su entrambi gli indici con ranking BM25 e snippet")
    void shouldSearchAcrossIndices() throws Exception {
        facade.indexCorpus(corpusDir);

        // Ricerca su Indice Contenuto: sia intro che machine_learning_ir contengono "retrieval"
        SearchResult contentResult = facade.search(TargetIndex.CONTENT, "retrieval", 10);
        assertNotNull(contentResult);
        assertEquals(2, contentResult.totalHits());
        assertEquals(2, contentResult.hits().size());
        assertTrue(contentResult.executionTimeMs() >= 0);
        assertNotNull(contentResult.hits().get(0).snippet());

        // Ricerca su Indice Nome File: solo information_retrieval_intro.txt contiene il token "retrieval"
        SearchResult filenameResult = facade.search(TargetIndex.FILENAME, "retrieval", 10);
        assertNotNull(filenameResult);
        assertEquals(1, filenameResult.totalHits());
        assertEquals(1, filenameResult.hits().size());
        assertTrue(filenameResult.hits().get(0).filename().contains("information_retrieval_intro.txt"));
    }

    @Test
    @DisplayName("Dovrebbe supportare query booleane e di frase tramite Facade")
    void shouldSupportBooleanAndPhraseQueries() throws Exception {
        facade.indexCorpus(corpusDir);

        // Frase esatta "information retrieval": presente in entrambi i file
        SearchResult phraseResult = facade.search(TargetIndex.CONTENT, "\"information retrieval\"", 10);
        assertEquals(2, phraseResult.totalHits());

        // Booleana con NOT: retrieval presente in intro e ml, ma ml contiene learning
        SearchResult booleanResult = facade.search(TargetIndex.CONTENT, "retrieval NOT learning", 10);
        assertEquals(1, booleanResult.totalHits());
        assertTrue(booleanResult.hits().get(0).filename().contains("information_retrieval_intro.txt"));
    }
}
