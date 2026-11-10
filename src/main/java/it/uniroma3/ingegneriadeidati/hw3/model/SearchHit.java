package it.uniroma3.ingegneriadeidati.hw3.model;

/**
 * Rappresenta un singolo risultato (hit) di una ricerca.
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
    @Override
    public String toString() {
        return String.format("[Score: %.4f | ID: %d] %s (%s) - %d bytes",
                score, docId, filename, relativePath, fileSize);
    }
}
