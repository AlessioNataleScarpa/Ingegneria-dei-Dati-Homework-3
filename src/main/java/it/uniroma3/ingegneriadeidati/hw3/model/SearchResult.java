package it.uniroma3.ingegneriadeidati.hw3.model;

import java.util.List;

/**
 * Risultato complessivo dell'esecuzione di una ricerca, comprensivo di metriche e hits.
 */
public record SearchResult(
        String queryText,
        String targetIndex,
        long totalHits,
        long executionTimeMs,
        List<SearchHit> hits
) {
}
