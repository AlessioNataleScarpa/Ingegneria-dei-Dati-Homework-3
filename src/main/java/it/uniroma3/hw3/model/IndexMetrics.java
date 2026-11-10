package it.uniroma3.hw3.model;

import java.util.Objects;

/**
 * Value Object contenente le statistiche di indicizzazione.
 * Applica il pattern Information Expert: implementa direttamente il calcolo
 * di throughput (documenti/sec, MB/sec) e compression ratio, evitando l'anemic domain model.
 */
public record IndexMetrics(
        TargetIndex targetIndex,
        int documentCount,
        long durationMs,
        long diskSizeBytes,
        long corpusSizeBytes
) {
    public IndexMetrics {
        Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");
        if (documentCount < 0) throw new IllegalArgumentException("documentCount non può essere negativo: " + documentCount);
        if (durationMs < 0) throw new IllegalArgumentException("durationMs non può essere negativo: " + durationMs);
        if (diskSizeBytes < 0) throw new IllegalArgumentException("diskSizeBytes non può essere negativo: " + diskSizeBytes);
        if (corpusSizeBytes < 0) throw new IllegalArgumentException("corpusSizeBytes non può essere negativo: " + corpusSizeBytes);
    }

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

    public double getCompressionRatio() {
        if (corpusSizeBytes <= 0) return 0.0;
        return (double) diskSizeBytes / corpusSizeBytes;
    }

    public String getFormattedDiskSize() {
        return formatBytes(diskSizeBytes);
    }

    public String getFormattedCorpusSize() {
        return formatBytes(corpusSizeBytes);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(java.util.Locale.US, "%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
