package it.uniroma3.hw3.model;

import java.util.Objects;

/**
 * Value Object che incapsula un singolo risultato di ricerca restituito da Lucene.
 * Applica il pattern Information Expert includendo la formattazione dei dati
 * per l'output su console e il calcolo della dimensione human-readable.
 */
public record SearchHit(
        int docId,
        float score,
        String filename,
        String relativePath,
        String absolutePath,
        long fileSize,
        String snippet
) {
    public SearchHit {
        if (docId < 0) {
            throw new IllegalArgumentException("docId non può essere negativo: " + docId);
        }
        if (score < 0.0f) {
            throw new IllegalArgumentException("score non può essere negativo: " + score);
        }
        Objects.requireNonNull(filename, "filename non può essere nullo");
        Objects.requireNonNull(relativePath, "relativePath non può essere nullo");
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize non può essere negativo: " + fileSize);
        }
    }

    /**
     * Formatta la dimensione del file in stringa leggibile (B, KB, MB, GB).
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(java.util.Locale.US, "%.2f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    /**
     * Rappresentazione tabellare allineata per la CLI.
     */
    public String toConsoleRow(int rank) {
        return String.format(java.util.Locale.US, "#%-5d | %-10.4f | %-35s | %s",
                rank, score, filename, relativePath);
    }
}
