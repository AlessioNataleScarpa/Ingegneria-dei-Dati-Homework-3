package it.uniroma3.ingegneriadeidati.hw3.benchmark;

import it.uniroma3.ingegneriadeidati.hw3.indexer.DocumentIndexer;
import it.uniroma3.ingegneriadeidati.hw3.model.IndexStats;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchResult;
import it.uniroma3.ingegneriadeidati.hw3.searcher.DocumentSearcher;
import it.uniroma3.ingegneriadeidati.hw3.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Suite di Benchmark sperimentale per l'indicizzazione e la ricerca.
 * Raccoglie metriche quantitative per la relazione tecnica finale:
 * - Tempi di indicizzazione per Filename e Content
 * - Dimensioni del corpus e degli indici su disco
 * - Throughput in docs/s e MB/s
 * - Latenza di esecuzione su 10+ query prefissate eterogenee (singolo termine, booleana AND/OR/NOT, frasi esatte)
 */
public class BenchmarkRunner {

    public record QueryBenchmarkResult(
            String queryText,
            String targetIndex,
            String queryType,
            long totalHits,
            double latencyMs
    ) {}

    public record BenchmarkReport(
            int fileCount,
            long corpusSizeBytes,
            IndexStats filenameStats,
            IndexStats contentStats,
            List<QueryBenchmarkResult> queryResults,
            double avgFilenameLatencyMs,
            double avgContentLatencyMs
    ) {
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("### Risultati Sperimentali del Benchmark\n\n");
            sb.append(String.format("- **Numero di documenti nel corpus**: %,d file\n", fileCount));
            sb.append(String.format("- **Dimensione totale corpus**: %s (%,d byte)\n\n",
                    FileUtils.formatBytes(corpusSizeBytes), corpusSizeBytes));

            sb.append("#### Metriche di Indicizzazione\n\n");
            sb.append("| Metrica | Indice A (Filename) | Indice B (Content) |\n");
            sb.append("| :--- | :--- | :--- |\n");
            sb.append(String.format("| **Tempo di Indicizzazione** | %d ms (%.3f s) | %d ms (%.3f s) |\n",
                    filenameStats.durationMs(), filenameStats.durationMs() / 1000.0,
                    contentStats.durationMs(), contentStats.durationMs() / 1000.0));
            sb.append(String.format("| **Dimensione Indice su Disco** | %s (%,d byte) | %s (%,d byte) |\n",
                    FileUtils.formatBytes(filenameStats.diskSizeBytes()), filenameStats.diskSizeBytes(),
                    FileUtils.formatBytes(contentStats.diskSizeBytes()), contentStats.diskSizeBytes()));
            sb.append(String.format("| **Throughput Documenti** | %.1f docs/s | %.1f docs/s |\n",
                    filenameStats.getThroughputDocsPerSec(), contentStats.getThroughputDocsPerSec()));
            sb.append(String.format("| **Throughput Dati** | %.2f MB/s | %.2f MB/s |\n\n",
                    filenameStats.getThroughputMBPerSec(), contentStats.getThroughputMBPerSec()));

            sb.append("#### Benchmark della Suite di Query (Latenza e Risultati)\n\n");
            sb.append("| ID | Query | Target | Tipologia | Hits | Latenza media (ms) |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
            int id = 1;
            for (QueryBenchmarkResult q : queryResults) {
                sb.append(String.format("| Q%02d | `%s` | %s | %s | %,d | %.3f ms |\n",
                        id++, q.queryText(), q.targetIndex(), q.queryType(), q.totalHits(), q.latencyMs()));
            }

            sb.append("\n");
            sb.append(String.format("- **Latenza media su Indice Nome**: %.3f ms\n", avgFilenameLatencyMs));
            sb.append(String.format("- **Latenza media su Indice Contenuto**: %.3f ms\n", avgContentLatencyMs));

            return sb.toString();
        }
    }

    /**
     * Esegue il benchmark completo.
     *
     * @param corpusDir       Directory contenente i file di input
     * @param filenameIndexDir Directory per l'indice del nome
     * @param contentIndexDir  Directory per l'indice del contenuto
     * @param iterations       Numero di ripetizioni per ciascuna query per mediare la latenza
     */
    public BenchmarkReport runBenchmark(Path corpusDir, Path filenameIndexDir, Path contentIndexDir, int iterations) throws Exception {
        DocumentIndexer indexer = new DocumentIndexer();

        System.out.println(">>> 1. Indicizzazione Filename Index...");
        IndexStats fnStats = indexer.buildFilenameIndex(corpusDir, filenameIndexDir);

        System.out.println(">>> 2. Indicizzazione Content Index...");
        IndexStats cntStats = indexer.buildContentIndex(corpusDir, contentIndexDir);

        long corpusSize = FileUtils.calculateDirectorySize(corpusDir);

        System.out.println(">>> 3. Esecuzione suite di query di benchmark...");
        List<QueryBenchmarkResult> qResults = new ArrayList<>();

        // Definizione della suite di query prefissate
        record QueryDef(String text, DocumentSearcher.TargetIndex target, String type) {}
        List<QueryDef> testQueries = List.of(
                // Query su Filename
                new QueryDef("relazione", DocumentSearcher.TargetIndex.FILENAME, "TermQuery singolo"),
                new QueryDef("ingegneria", DocumentSearcher.TargetIndex.FILENAME, "TermQuery singolo"),
                new QueryDef("ingegneria AND dati", DocumentSearcher.TargetIndex.FILENAME, "BooleanQuery AND"),
                new QueryDef("cloud OR sicurezza", DocumentSearcher.TargetIndex.FILENAME, "BooleanQuery OR"),
                new QueryDef("relazione AND NOT appunti", DocumentSearcher.TargetIndex.FILENAME, "BooleanQuery NOT"),
                new QueryDef("\"ingegneria dei dati\"", DocumentSearcher.TargetIndex.FILENAME, "PhraseQuery esatta"),

                // Query su Content
                new QueryDef("database", DocumentSearcher.TargetIndex.CONTENT, "TermQuery singolo"),
                new QueryDef("algoritmo", DocumentSearcher.TargetIndex.CONTENT, "TermQuery singolo"),
                new QueryDef("database AND ottimizzazione", DocumentSearcher.TargetIndex.CONTENT, "BooleanQuery AND"),
                new QueryDef("concorrenza OR crittografia", DocumentSearcher.TargetIndex.CONTENT, "BooleanQuery OR"),
                new QueryDef("algoritmo AND NOT deadlock", DocumentSearcher.TargetIndex.CONTENT, "BooleanQuery NOT"),
                new QueryDef("\"indice inverso\"", DocumentSearcher.TargetIndex.CONTENT, "PhraseQuery esatta")
        );

        try (DocumentSearcher searcher = new DocumentSearcher(filenameIndexDir, contentIndexDir)) {
            // Warm-up JVM
            for (QueryDef q : testQueries) {
                try {
                    searcher.search(q.target(), q.text(), 10);
                } catch (Exception ignored) {}
            }

            // Misurazioni
            for (QueryDef q : testQueries) {
                long totalTimeNs = 0L;
                long hits = 0L;

                for (int it = 0; it < iterations; it++) {
                    long t0 = System.nanoTime();
                    SearchResult res = searcher.search(q.target(), q.text(), 20);
                    long dt = System.nanoTime() - t0;
                    totalTimeNs += dt;
                    hits = res.totalHits();
                }

                double avgMs = (totalTimeNs / 1_000_000.0) / iterations;
                String targetStr = (q.target() == DocumentSearcher.TargetIndex.FILENAME) ? "Filename" : "Content";
                qResults.add(new QueryBenchmarkResult(q.text(), targetStr, q.type(), hits, avgMs));
            }
        }

        double sumFn = 0; int countFn = 0;
        double sumCnt = 0; int countCnt = 0;
        for (QueryBenchmarkResult r : qResults) {
            if ("Filename".equals(r.targetIndex())) {
                sumFn += r.latencyMs();
                countFn++;
            } else {
                sumCnt += r.latencyMs();
                countCnt++;
            }
        }

        double avgFn = countFn > 0 ? sumFn / countFn : 0.0;
        double avgCnt = countCnt > 0 ? sumCnt / countCnt : 0.0;

        return new BenchmarkReport(
                fnStats.documentCount(),
                corpusSize,
                fnStats,
                cntStats,
                qResults,
                avgFn,
                avgCnt
        );
    }
}
