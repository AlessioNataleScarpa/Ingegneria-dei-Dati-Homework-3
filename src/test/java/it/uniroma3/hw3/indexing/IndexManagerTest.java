package it.uniroma3.hw3.indexing;

import it.uniroma3.hw3.analysis.AnalyzerStrategy;
import it.uniroma3.hw3.analysis.DefaultAnalyzerProvider;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Suite per il Sottosistema di Indicizzazione (it.uniroma3.hw3.indexing)")
class IndexManagerTest {

    @TempDir
    Path tempDir;

    private Path corpusDir;
    private Path filenameIndexDir;
    private Path contentIndexDir;

    private AnalyzerStrategy analyzerStrategy;
    private IndexManager indexManager;

    @BeforeEach
    void setUp() throws IOException {
        corpusDir = tempDir.resolve("corpus");
        filenameIndexDir = tempDir.resolve("filename_index");
        contentIndexDir = tempDir.resolve("content_index");
        Files.createDirectories(corpusDir);

        analyzerStrategy = new DefaultAnalyzerProvider();
        indexManager = new LuceneIndexManager(analyzerStrategy);
    }

    @AfterEach
    void tearDown() {
        if (analyzerStrategy != null) {
            analyzerStrategy.close();
        }
    }

    private void createSampleCorpus() throws IOException {
        Files.writeString(
                corpusDir.resolve("relazione_ingegneria_dati-v1.txt"),
                "Questo documento contiene informazioni sull'architettura e l'algoritmo di indicizzazione."
        );
        Files.writeString(
                corpusDir.resolve("specifica-sistemi_distribuiti.txt"),
                "I sistemi distribuiti richiedono algoritmi di consenso robusti e scalabili."
        );
        Files.writeString(
                corpusDir.resolve("appunti_progettazione_web.txt"),
                "La progettazione web moderna utilizza standard aperti e architetture scalabili."
        );

        // Creiamo anche una sottocartella con un ulteriore file .txt e un file non .txt da ignorare
        Path subDir = corpusDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(
                subDir.resolve("manuale-sviluppatore.txt"),
                "Guida per gli sviluppatori di motori di ricerca e Information Retrieval."
        );
        Files.writeString(
                subDir.resolve("immagine.png"),
                "Dati binari fittizi da ignorare"
        );
    }

    @Test
    @DisplayName("Indicizzazione Filename: crea indice dei nomi e popola correttamente i metadati")
    void testBuildFilenameIndex() throws IOException, IndexingException {
        createSampleCorpus();

        IndexMetrics metrics = indexManager.index(corpusDir, filenameIndexDir, TargetIndex.FILENAME);

        assertNotNull(metrics);
        assertEquals(TargetIndex.FILENAME, metrics.targetIndex());
        assertEquals(4, metrics.documentCount(), "Devono essere indicizzati esattamente 4 file .txt");
        assertTrue(metrics.durationMs() >= 0);
        assertTrue(metrics.diskSizeBytes() > 0, "L'indice su disco deve avere dimensione > 0");
        assertTrue(metrics.corpusSizeBytes() > 0);

        // Ispezione dell'indice Lucene generato
        try (Directory dir = FSDirectory.open(filenameIndexDir);
             IndexReader reader = DirectoryReader.open(dir)) {
            assertEquals(4, reader.numDocs(), "L'indice Lucene deve contenere esattamente 4 documenti");

            for (int i = 0; i < reader.numDocs(); i++) {
                var doc = reader.storedFields().document(i);
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_FILENAME), "FIELD_FILENAME memorizzato");
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_PATH), "FIELD_PATH memorizzato");
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_ABSOLUTE_PATH), "FIELD_ABSOLUTE_PATH memorizzato");
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_FILE_SIZE), "FIELD_FILE_SIZE memorizzato");
                assertNull(doc.get(LuceneDocumentBuilder.FIELD_CONTENT), "FIELD_CONTENT non deve essere presente in Filename Index");
            }
        }
    }

    @Test
    @DisplayName("Indicizzazione Content: crea indice dei contenuti con streaming UTF-8 e memorizza metadati")
    void testBuildContentIndex() throws IOException, IndexingException {
        createSampleCorpus();

        IndexMetrics metrics = indexManager.index(corpusDir, contentIndexDir, TargetIndex.CONTENT);

        assertNotNull(metrics);
        assertEquals(TargetIndex.CONTENT, metrics.targetIndex());
        assertEquals(4, metrics.documentCount());
        assertTrue(metrics.diskSizeBytes() > 0);

        try (Directory dir = FSDirectory.open(contentIndexDir);
             IndexReader reader = DirectoryReader.open(dir)) {
            assertEquals(4, reader.numDocs());

            for (int i = 0; i < reader.numDocs(); i++) {
                var doc = reader.storedFields().document(i);
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_FILENAME));
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_PATH));
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_ABSOLUTE_PATH));
                assertNotNull(doc.get(LuceneDocumentBuilder.FIELD_FILE_SIZE));
                // Il contenuto è indicizzato (TextField Reader) ma non memorizzato
                assertNull(doc.get(LuceneDocumentBuilder.FIELD_CONTENT));
            }
        }
    }

    @Test
    @DisplayName("indexAll: indicizza contemporaneamente entrambi gli indici fisici")
    void testIndexAll() throws IOException, IndexingException {
        createSampleCorpus();

        List<IndexMetrics> allMetrics = indexManager.indexAll(corpusDir, filenameIndexDir, contentIndexDir);

        assertEquals(2, allMetrics.size());
        assertEquals(TargetIndex.FILENAME, allMetrics.get(0).targetIndex());
        assertEquals(TargetIndex.CONTENT, allMetrics.get(1).targetIndex());

        assertTrue(Files.exists(filenameIndexDir.resolve("write.lock")) || Files.list(filenameIndexDir).findAny().isPresent());
        assertTrue(Files.exists(contentIndexDir.resolve("write.lock")) || Files.list(contentIndexDir).findAny().isPresent());
    }

    @Test
    @DisplayName("Corpus non valido o vuoto: solleva eccezioni appropriate")
    void testInvalidCorpus() {
        Path notExisting = tempDir.resolve("non_esiste");
        assertThrows(IndexingException.class, () ->
                indexManager.index(notExisting, filenameIndexDir, TargetIndex.FILENAME)
        );
    }
}
