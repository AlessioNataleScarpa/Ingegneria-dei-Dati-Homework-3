package it.uniroma3.hw3.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Suite: CorpusDatasetManager (Parsing Formati Letteratura IR)")
class CorpusDatasetManagerTest {

    private CorpusDatasetManager datasetManager;

    @BeforeEach
    void setUp() {
        datasetManager = new CorpusDatasetManager();
    }

    @Test
    @DisplayName("Dovrebbe sanificare correttamente i nomi dei file rimuovendo caratteri non validi")
    void testSanitizeFileName() {
        assertEquals("experimental_investigation_of_aerodynamics",
                datasetManager.sanitizeFileName("Experimental investigation of aerodynamics."));
        assertEquals("dewey_decimal_classifications_18th_ed",
                datasetManager.sanitizeFileName("Dewey: Decimal Classifications / 18th Ed."));
        assertEquals("", datasetManager.sanitizeFileName(null));
        assertEquals("", datasetManager.sanitizeFileName("   "));
    }

    @Test
    @DisplayName("Dovrebbe estrarre correttamente documenti in formato tag (.I, .T, .W) in file .txt autonomi")
    void testExtractCisiOrCranfield(@TempDir Path tempDir) throws IOException {
        String mockRawContent = """
                .I 1
                .T
                18 Editions of the Dewey Decimal Classifications
                .A
                Comaromi, J.P.
                .W
                The present study is a history of the Dewey Decimal Classification.
                The first edition of the DDC was published in 1876.
                .I 2
                .T
                Use of Information Retrieval Systems
                .A
                Salton, G.
                .W
                Information storage and retrieval systems have revolutionized computer science.
                Experimental evaluation provides meaningful metrics.
                """;

        Path masterFile = tempDir.resolve("mock_cisi.raw");
        Files.writeString(masterFile, mockRawContent, StandardCharsets.UTF_8);

        Path outputDir = tempDir.resolve("output_txt");
        Files.createDirectories(outputDir);

        int extractedCount = datasetManager.extractCisiOrCranfield(masterFile, outputDir, "cisi");

        assertEquals(2, extractedCount);

        try (var stream = Files.list(outputDir)) {
            var files = stream.toList();
            assertEquals(2, files.size());

            // Verifica che il primo file esista e contenga il testo atteso
            Path file1 = files.stream()
                    .filter(p -> p.getFileName().toString().startsWith("cisi_0001_"))
                    .findFirst()
                    .orElseThrow();

            String content1 = Files.readString(file1);
            assertTrue(content1.contains("The present study is a history of the Dewey Decimal Classification."));
            assertTrue(file1.getFileName().toString().contains("dewey_decimal"));

            // Verifica che il secondo file esista
            Path file2 = files.stream()
                    .filter(p -> p.getFileName().toString().startsWith("cisi_0002_"))
                    .findFirst()
                    .orElseThrow();

            String content2 = Files.readString(file2);
            assertTrue(content2.contains("Information storage and retrieval systems"));
        }
    }
}
