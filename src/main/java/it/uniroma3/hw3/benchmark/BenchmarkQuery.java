package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.model.TargetIndex;

import java.util.Objects;

/**
 * Value Object che incapsula una singola query di benchmark calibrata per la valutazione del motore di ricerca.
 * Include la tracciabilità della fonte autentica di letteratura da cui è stata derivata.
 *
 * @param queryText        testo della query da sottoporre al QueryParser
 * @param targetIndex      indice bersaglio (FILENAME o CONTENT)
 * @param queryType        tipologia algoritmica (Term, Boolean AND/OR/NOT, Phrase, Stemming)
 * @param literatureSource descrizione del topic o documento di letteratura da cui è tratta
 */
public record BenchmarkQuery(
        String queryText,
        TargetIndex targetIndex,
        String queryType,
        String literatureSource
) {
    public BenchmarkQuery {
        Objects.requireNonNull(queryText, "queryText non può essere nullo");
        Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");
        Objects.requireNonNull(queryType, "queryType non può essere nullo");
        Objects.requireNonNull(literatureSource, "literatureSource non può essere nullo");
    }
}
