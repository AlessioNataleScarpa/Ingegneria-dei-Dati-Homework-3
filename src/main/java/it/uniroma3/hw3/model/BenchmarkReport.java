package it.uniroma3.hw3.model;

import java.util.List;
import java.util.Objects;

/**
 * Value Object aggregato per i risultati della suite di benchmark.
 * Implementa la serializzazione in formato Markdown per il report finale.
 */
public record BenchmarkReport(
        int fileCount,
        long corpusSizeBytes,
        IndexMetrics filenameMetrics,
        IndexMetrics contentMetrics,
        List<QueryBenchmarkResult> queryResults,
        double avgFilenameLatencyMs,
        double avgContentLatencyMs
) {
    public record QueryBenchmarkResult(
            String queryText,
            TargetIndex targetIndex,
            String queryType,
            long totalHits,
            double latencyMs
    ) {
        public QueryBenchmarkResult {
            Objects.requireNonNull(queryText, "queryText non può essere nullo");
            Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");
            Objects.requireNonNull(queryType, "queryType non può essere nullo");
        }
    }

    public BenchmarkReport {
        Objects.requireNonNull(filenameMetrics, "filenameMetrics non può essere nullo");
        Objects.requireNonNull(contentMetrics, "contentMetrics non può essere nullo");
        queryResults = queryResults == null ? List.of() : List.copyOf(queryResults);
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("### Risultati Sperimentali del Benchmark\n\n");
        sb.append(String.format("- **Numero di documenti nel corpus**: %,d file\n", fileCount));
        sb.append(String.format("- **Dimensione totale corpus**: %s (%,d byte)\n\n",
                filenameMetrics.getFormattedCorpusSize(), corpusSizeBytes));

        sb.append("#### Metriche di Indicizzazione\n\n");
        sb.append("| Metrica | Indice A (Filename) | Indice B (Content) |\n");
        sb.append("| :--- | :--- | :--- |\n");
        sb.append(String.format("| **Tempo di Indicizzazione** | %d ms (%.3f s) | %d ms (%.3f s) |\n",
                filenameMetrics.durationMs(), filenameMetrics.durationMs() / 1000.0,
                contentMetrics.durationMs(), contentMetrics.durationMs() / 1000.0));
        sb.append(String.format("| **Dimensione Indice su Disco** | %s (%,d byte) | %s (%,d byte) |\n",
                filenameMetrics.getFormattedDiskSize(), filenameMetrics.diskSizeBytes(),
                contentMetrics.getFormattedDiskSize(), contentMetrics.diskSizeBytes()));
        sb.append(String.format("| **Throughput Documenti** | %.1f docs/s | %.1f docs/s |\n",
                filenameMetrics.getThroughputDocsPerSec(), contentMetrics.getThroughputDocsPerSec()));
        sb.append(String.format("| **Throughput Dati** | %.2f MB/s | %.2f MB/s |\n\n",
                filenameMetrics.getThroughputMBPerSec(), contentMetrics.getThroughputMBPerSec()));

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
