package it.uniroma3.ingegneriadeidati.hw3.indexer;

import it.uniroma3.ingegneriadeidati.hw3.benchmark.DatasetGenerator;
import it.uniroma3.ingegneriadeidati.hw3.model.IndexStats;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchResult;
import it.uniroma3.ingegneriadeidati.hw3.searcher.DocumentSearcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexerAndSearcherTest {

    @TempDir
    static Path tempDir;

    private static Path corpusDir;
    private static Path filenameIndexDir;
    private static Path contentIndexDir;

    @BeforeAll
    static void setup() throws Exception {
        corpusDir = tempDir.resolve("test_corpus");
        filenameIndexDir = tempDir.resolve("test_filename_idx");
        contentIndexDir = tempDir.resolve("test_content_idx");

        // Creiamo un file di test con contenuto noto
        Files.createDirectories(corpusDir);
        Path file1 = corpusDir.resolve("relazione_database_sistemi.txt");
        Files.writeString(file1, "Questa e una relazione sul database relazionale e sulle transazioni ACID.");

        Path file2 = corpusDir.resolve("appunti_algoritmi_distribuiti.txt");
        Files.writeString(file2, "Note sul clustering distribuito, deadlock e concorrenza dei processi.");

        // Creiamo anche 20 documenti sintetici
        DatasetGenerator.generateDataset(corpusDir, 20, 50, 12345L);

        // Indicizziamo
        DocumentIndexer indexer = new DocumentIndexer();
        List<IndexStats> stats = indexer.indexCorpus(corpusDir, filenameIndexDir, contentIndexDir);
        assertEquals(2, stats.size());
    }

    @Test
    @DisplayName("Filename Search: TermQuery su nome file")
    void testFilenameTermQuery() throws Exception {
        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            SearchResult res = searcher.search(DocumentSearcher.TargetIndex.FILENAME, "database", 10);
            assertTrue(res.totalHits() > 0, "Dovrebbe trovare almeno il file relazione_database_sistemi.txt");
            assertTrue(res.hits().stream().anyMatch(h -> h.filename().contains("database")));
        }
    }

    @Test
    @DisplayName("Filename Search: BooleanQuery AND su nome file")
    void testFilenameBooleanQuery() throws Exception {
        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            SearchResult res = searcher.search(DocumentSearcher.TargetIndex.FILENAME, "relazione AND database", 10);
            assertTrue(res.totalHits() >= 1);
            assertEquals("relazione_database_sistemi.txt", res.hits().getFirst().filename());
        }
    }

    @Test
    @DisplayName("Content Search: TermQuery su contenuto")
    void testContentTermQuery() throws Exception {
        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            SearchResult res = searcher.search(DocumentSearcher.TargetIndex.CONTENT, "clustering", 10);
            assertTrue(res.totalHits() > 0);
            assertTrue(res.hits().stream().anyMatch(h -> h.filename().contains("algoritmi")));
        }
    }

    @Test
    @DisplayName("Content Search: PhraseQuery esatta su contenuto")
    void testContentPhraseQuery() throws Exception {
        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            SearchResult res = searcher.search(DocumentSearcher.TargetIndex.CONTENT, "\"transazioni acid\"", 10);
            assertTrue(res.totalHits() >= 1);
            assertEquals("relazione_database_sistemi.txt", res.hits().getFirst().filename());
        }
    }

    @Test
    @DisplayName("Content Search: BooleanQuery con operatore NOT")
    void testContentBooleanNotQuery() throws Exception {
        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            SearchResult res = searcher.search(DocumentSearcher.TargetIndex.CONTENT, "clustering AND NOT transazioni", 10);
            assertTrue(res.totalHits() >= 1);
            assertFalse(res.hits().stream().anyMatch(h -> h.filename().contains("relazione_database_sistemi")));
        }
    }
}
