package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.model.BenchmarkReport;

import java.util.Map;
import java.util.Objects;

/**
 * Value Object aggregato che raccoglie i risultati dei benchmark eseguiti sui molteplici corpora di letteratura.
 * Genera il report comparativo formattato in Markdown per la Relazione Tecnica finale.
 */
public record MultiDomainBenchmarkReport(
        Map<BenchmarkCorpus, BenchmarkReport> domainReports
) {
    public MultiDomainBenchmarkReport {
        Objects.requireNonNull(domainReports, "domainReports non può essere nullo");
        domainReports = Map.copyOf(domainReports);
    }

    /**
     * Serializza il report comparativo multi-dominio in formato Markdown.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Analisi Sperimentale e Benchmark Comparativo Multi-Dominio\n\n");
        sb.append("La valutazione sperimentale è stata condotta su tre collezioni standard della letteratura di Information Retrieval:\n");
        sb.append("1. **CISI** (*Information Science & Biblioteconomia* - 1.460 abstract)\n");
        sb.append("2. **Cranfield** (*Ingegneria Aeronautica* - 1.400 abstract)\n");
        sb.append("3. **20 Newsgroups (Comp/Sci)** (*Informatica, Hardware e Crittografia* - ~1.500 post)\n\n");

        sb.append("### 1. Confronto delle Prestazioni di Indicizzazione\n\n");
        sb.append("| Dataset | Documenti | Dimensione Corpus | Tempo Indice A (Filename) | Tempo Indice B (Content) | Dim. Indice A | Dim. Indice B | Throughput Docs/s (Content) |\n");
        sb.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n");

        for (var entry : domainReports.entrySet()) {
            BenchmarkCorpus corpus = entry.getKey();
            BenchmarkReport report = entry.getValue();
            sb.append(String.format("| **%s** | %,d | %s | %d ms | %d ms | %s | %s | %.1f docs/s |\n",
                    corpus.getDisplayName(),
                    report.fileCount(),
                    report.filenameMetrics().getFormattedCorpusSize(),
                    report.filenameMetrics().durationMs(),
                    report.contentMetrics().durationMs(),
                    report.filenameMetrics().getFormattedDiskSize(),
                    report.contentMetrics().getFormattedDiskSize(),
                    report.contentMetrics().getThroughputDocsPerSec()));
        }

        sb.append("\n");
        sb.append("### 2. Confronto delle Latenze Medie di Interrogazione\n\n");
        sb.append("| Dataset | Latenza Media Indice A (Filename) | Latenza Media Indice B (Content) |\n");
        sb.append("| :--- | :---: | :---: |\n");

        for (var entry : domainReports.entrySet()) {
            BenchmarkCorpus corpus = entry.getKey();
            BenchmarkReport report = entry.getValue();
            sb.append(String.format("| **%s** | %.3f ms | %.3f ms |\n",
                    corpus.getDisplayName(),
                    report.avgFilenameLatencyMs(),
                    report.avgContentLatencyMs()));
        }

        sb.append("\n");
        sb.append("### 3. Dettaglio delle Query Autentiche per Dominio\n\n");

        for (var entry : domainReports.entrySet()) {
            BenchmarkCorpus corpus = entry.getKey();
            BenchmarkReport report = entry.getValue();

            sb.append(String.format("#### Dominio: %s (%s)\n\n", corpus.getDisplayName(), corpus.getDomain()));
            sb.append("| ID | Query | Target | Tipologia | Hits | Latenza (ms) |\n");
            sb.append("| :--- | :--- | :---: | :--- | :---: | :---: |\n");

            int qId = 1;
            for (var qr : report.queryResults()) {
                sb.append(String.format("| Q%02d | `%s` | %s | %s | %,d | %.3f ms |\n",
                        qId++,
                        qr.queryText(),
                        qr.targetIndex(),
                        qr.queryType(),
                        qr.totalHits(),
                        qr.latencyMs()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
