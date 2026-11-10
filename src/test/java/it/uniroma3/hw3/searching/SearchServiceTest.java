package it.uniroma3.hw3.searching;

import it.uniroma3.hw3.analysis.DefaultAnalyzerProvider;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.indexing.IndexManager;
import it.uniroma3.hw3.indexing.LuceneDocumentBuilder;
import it.uniroma3.hw3.indexing.LuceneIndexManager;
import it.uniroma3.hw3.model.SearchHit;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchService - Test del Sottosistema di Ricerca")
class SearchServiceTest {

    @TempDir
    Path tempDir;

    private Path corpusDir;
    private Path filenameIndexPath;
    private Path contentIndexPath;
    private DefaultAnalyzerProvider analyzerProvider;
    private LuceneSearchService searchService;

    @BeforeEach
    void setUp() throws Exception {
        corpusDir = tempDir.resolve("corpus");
        filenameIndexPath = tempDir.resolve("indexes").resolve("filename_index");
        contentIndexPath = tempDir.resolve("indexes").resolve("content_index");
        Files.createDirectories(corpusDir);

        // Creazione di file di test nel corpus con contenuti e nomi specifici
        createTestFile("algoritmi-avanzati.txt", "Questo documento tratta di algoritmi distribuiti e strutture dati per motori di ricerca.");
        createTestFile("database_relazionali.txt", "I database relazionali utilizzano query SQL avanzate e indici B-Tree.");
        createTestFile("sistemi_informativi.txt", "L'architettura dei sistemi informativi moderni e il recupero delle informazioni mediante indexing.");
        createTestFile("note.txt", "Semplici note di studio su ingegneria dei dati.");

        // Indicizzazione del corpus su entrambi gli indici
        analyzerProvider = new DefaultAnalyzerProvider();
        IndexManager indexManager = new LuceneIndexManager(analyzerProvider, new LuceneDocumentBuilder(), 64.0);
        indexManager.indexAll(corpusDir, filenameIndexPath, contentIndexPath);

        searchService = new LuceneSearchService(filenameIndexPath, contentIndexPath, analyzerProvider);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (searchService != null) {
            searchService.close();
        }
        if (analyzerProvider != null) {
            analyzerProvider.close();
        }
    }

    private void createTestFile(String filename, String content) throws IOException {
        Path filePath = corpusDir.resolve(filename);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("TermQuery su Indice Nome File trova il documento corrispondente")
    void testSearchFilenameTermQuery() throws SearchException {
        SearchResult result = searchService.search(TargetIndex.FILENAME, "algoritmi", 10);

        assertTrue(result.hasHits(), "Dovrebbe trovare almeno una corrispondenza per 'algoritmi'");
        assertEquals(1, result.getHitsCount());

        SearchHit hit = result.getHit(0);
        assertEquals("algoritmi-avanzati.txt", hit.filename());
        assertTrue(hit.score() > 0.0f);
    }

    @Test
    @DisplayName("BooleanQuery con AND e NOT su Indice Nome File")
    void testSearchFilenameBooleanQuery() throws SearchException {
        // Query AND
        SearchResult andResult = searchService.search(TargetIndex.FILENAME, "database AND relazionali", 10);
        assertTrue(andResult.hasHits());
        assertEquals("database_relazionali.txt", andResult.getHit(0).filename());

        // Query NOT
        SearchResult notResult = searchService.search(TargetIndex.FILENAME, "algoritmi NOT avanzati", 10);
        assertFalse(notResult.hasHits(), "Non dovrebbe trovare hit poiché 'avanzati' è escluso");
    }

    @Test
    @DisplayName("TermQuery su Indice Contenuto con Stemming in lingua italiana")
    void testSearchContentWithStemming() throws SearchException {
        // "sistemi_informativi.txt" contiene "recupero delle informazioni"
        // La ricerca per singolare "informazione" deve fare match grazie allo stemmer italiano
        SearchResult result = searchService.search(TargetIndex.CONTENT, "informazione", 10);

        assertTrue(result.hasHits(), "Lo stemming dovrebbe ricondurre 'informazione' a 'informazioni'");
        assertEquals(1, result.getHitsCount());
        assertEquals("sistemi_informativi.txt", result.getHit(0).filename());
        assertFalse(result.getHit(0).snippet().isBlank(), "Lo snippet non dovrebbe essere vuoto");
    }

    @Test
    @DisplayName("PhraseQuery su Indice Contenuto con termini contigui tra apici")
    void testSearchContentPhraseQuery() throws SearchException {
        // "Questo documento tratta di algoritmi distribuiti e strutture dati..."
        SearchResult phraseResult = searchService.search(TargetIndex.CONTENT, "\"strutture dati\"", 10);

        assertTrue(phraseResult.hasHits());
        assertEquals("algoritmi-avanzati.txt", phraseResult.getHit(0).filename());

        // Ricerca di termini invertiti come frase esatta non deve produrre match
        SearchResult nonMatchingPhrase = searchService.search(TargetIndex.CONTENT, "\"dati strutture\"", 10);
        assertFalse(nonMatchingPhrase.hasHits());
    }

    @Test
    @DisplayName("searchByQuery con istanza tipizzata Query Lucene")
    void testSearchByQueryObject() throws SearchException {
        Query termQuery = new TermQuery(new Term(LuceneDocumentBuilder.FIELD_FILENAME, "note"));
        SearchResult result = searchService.searchByQuery(TargetIndex.FILENAME, termQuery, 5);

        assertTrue(result.hasHits());
        assertEquals("note.txt", result.getHit(0).filename());
    }

    @Test
    @DisplayName("Gestione stringa vuota o spazi bianchi")
    void testEmptyQueryHandling() throws SearchException {
        SearchResult result = searchService.search(TargetIndex.CONTENT, "   ", 10);
        assertFalse(result.hasHits());
        assertEquals(0, result.totalHits());
    }

    @Test
    @DisplayName("Sintassi query non valida genera SearchException")
    void testInvalidQueryThrowsSearchException() {
        assertThrows(SearchException.class, () ->
                searchService.search(TargetIndex.CONTENT, "AND OR *?", 10)
        );
    }

    @Test
    @DisplayName("Chiamata a close() impedisce ricerche successive")
    void testCloseReleasesResources() throws SearchException {
        searchService.close();

        assertThrows(IllegalStateException.class, () ->
                searchService.search(TargetIndex.FILENAME, "test", 10)
        );
    }
}
