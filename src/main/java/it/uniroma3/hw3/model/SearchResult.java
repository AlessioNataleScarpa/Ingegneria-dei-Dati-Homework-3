package it.uniroma3.hw3.model;

import java.util.List;
import java.util.Objects;

/**
 * Entità aggregata del risultato di una ricerca.
 * Garantisce l'immutabilità della collezione delle hits attraverso copia difensiva (List.copyOf).
 */
public record SearchResult(
        String queryText,
        TargetIndex targetIndex,
        long totalHits,
        long executionTimeMs,
        List<SearchHit> hits
) {
    public SearchResult {
        Objects.requireNonNull(queryText, "queryText non può essere nullo");
        Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");
        if (totalHits < 0) {
            throw new IllegalArgumentException("totalHits non può essere negativo: " + totalHits);
        }
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs non può essere negativo: " + executionTimeMs);
        }
        hits = hits == null ? List.of() : List.copyOf(hits);
    }

    public boolean hasHits() {
        return totalHits > 0 && !hits.isEmpty();
    }

    public int getHitsCount() {
        return hits.size();
    }

    public SearchHit getHit(int index) {
        return hits.get(index);
    }
}
