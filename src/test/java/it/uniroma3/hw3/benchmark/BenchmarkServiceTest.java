package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.analysis.DefaultAnalyzerProvider;
import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.indexing.LuceneDocumentBuilder;
import it.uniroma3.hw3.indexing.LuceneIndexManager;
import it.uniroma3.hw3.model.BenchmarkReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Suite: BenchmarkService (End-to-End Benchmark e Report Markdown)")
class BenchmarkServiceTest {

    @Test
    @DisplayName("Dovrebbe eseguire il benchmark su un corpus simulato, misurare metriche e produrre report valido")
    void testRunBenchmarkOnSimulatedCorpus(@TempDir Path tempDir) throws IOException, BenchmarkException {
        // Prepariamo una directory corpus simulata con file .txt conformi alle convenzioni CISI
        Path corpusDir = tempDir.resolve("cisi");
        Files.createDirectories(corpusDir);

        Path file1 = corpusDir.resolve("cisi_0001_18_editions_of_dewey_decimal_classifications.txt");
        Files.writeString(file1, "The present study is a history of the Dewey Decimal Classification. Information retrieval systems.", StandardCharsets.UTF_8);

        Path file2 = corpusDir.resolve("cisi_0002_use_of_information_retrieval_systems.txt");
        Files.writeString(file2, "Information storage and retrieval systems have revolutionized computer systems indexing and searching.", StandardCharsets.UTF_8);

        Path file3 = corpusDir.resolve("cisi_0003_computer_systems_in_libraries.txt");
        Files.writeString(file3, "Computer systems in libraries provide automated cataloging and advanced indexing mechanisms.", StandardCharsets.UTF_8);

        // Subclassiamo CorpusDatasetManager per bypassare il download HTTP durante i test unitari
        CorpusDatasetManager mockDatasetManager = new CorpusDatasetManager() {
            @Override
            public Path prepareCorpus(BenchmarkCorpus corpus, Path baseDataDir) {
                return corpusDir;
            }
        };

        DefaultAnalyzerProvider analyzerProvider = new DefaultAnalyzerProvider();
        LuceneIndexManager indexManager = new LuceneIndexManager(analyzerProvider);

        DefaultBenchmarkService benchmarkService = new DefaultBenchmarkService(
                mockDatasetManager,
                analyzerProvider,
                indexManager
        );

        Path baseIndexDir = tempDir.resolve("indexes");
        Files.createDirectories(baseIndexDir);

        BenchmarkReport report = benchmarkService.runBenchmark(BenchmarkCorpus.CISI, tempDir, baseIndexDir);

        assertNotNull(report, "Il report non deve essere nullo");
        assertEquals(3, report.fileCount(), "Devono essere stati indicizzati 3 file");
        assertTrue(report.corpusSizeBytes() > 0, "La dimensione del corpus deve essere maggiore di 0");

        // Verifica Metriche Indice A e B
        assertNotNull(report.filenameMetrics());
        assertNotNull(report.contentMetrics());
        assertTrue(report.filenameMetrics().durationMs() >= 0);
        assertTrue(report.contentMetrics().durationMs() >= 0);
        assertTrue(report.filenameMetrics().diskSizeBytes() > 0);
        assertTrue(report.contentMetrics().diskSizeBytes() > 0);

        // Verifica risultati delle query
        assertFalse(report.queryResults().isEmpty(), "Le query calibrate devono essere state eseguite");
        assertTrue(report.avgFilenameLatencyMs() >= 0.0);
        assertTrue(report.avgContentLatencyMs() >= 0.0);

        // Verifica generazione Markdown
        String markdown = report.toMarkdown();
        assertNotNull(markdown);
        assertTrue(markdown.contains("### Risultati Sperimentali del Benchmark"));
        assertTrue(markdown.contains("Metriche di Indicizzazione"));

        // Verifica MultiDomainBenchmarkReport
        MultiDomainBenchmarkReport multiReport = new MultiDomainBenchmarkReport(Map.of(BenchmarkCorpus.CISI, report));
        String multiMarkdown = multiReport.toMarkdown();
        assertTrue(multiMarkdown.contains("Analisi Sperimentale e Benchmark Comparativo Multi-Dominio"));
        assertTrue(multiMarkdown.contains("CISI"));
    }
}
