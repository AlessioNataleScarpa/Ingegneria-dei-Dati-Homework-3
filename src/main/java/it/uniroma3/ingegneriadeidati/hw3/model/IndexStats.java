package it.uniroma3.ingegneriadeidati.hw3.model;

/**
 * Metriche rilevate durante la fase di indicizzazione di un corpus.
 */
public record IndexStats(
        String indexName,
        int documentCount,
        long durationMs,
        long diskSizeBytes,
        long corpusSizeBytes
) {
    public double getThroughputDocsPerSec() {
        if (durationMs <= 0) return 0.0;
        return (documentCount * 1000.0) / durationMs;
    }

    public double getThroughputMBPerSec() {
        if (durationMs <= 0) return 0.0;
        return ((corpusSizeBytes / (1024.0 * 1024.0)) * 1000.0) / durationMs;
    }

    public double getDiskSizeMB() {
        return diskSizeBytes / (1024.0 * 1024.0);
    }

    public double getCorpusSizeMB() {
        return corpusSizeBytes / (1024.0 * 1024.0);
    }
}
