package it.uniroma3.ingegneriadeidati.hw3.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.pattern.PatternTokenizer;

import java.util.regex.Pattern;

/**
 * Custom Analyzer per l'indicizzazione dei nomi di file (Filename Index).
 * <p>
 * Caratteristiche principali:
 * 1. Tokenizzazione basata su espressioni regolari (PatternTokenizer):
 *    spezza la stringa del nome file su delimitatori convenzionali come punti (.), trattini (-),
 *    trattini bassi (_), spazi (\s) e caratteri non alfanumerici.
 * 2. Normalizzazione case-insensitive: applicazione del filtro LowerCaseFilter.
 * 3. Assenza di stop-words e stemming: i componenti dei nomi di file sono spesso codici, acronimi,
 *    estensioni o parole chiave esatte che non devono subire alterazioni morfologiche.
 * </p>
 */
public class FilenameAnalyzer extends Analyzer {

    /**
     * Regex che isola i delimitatori comuni nei percorsi e nomi di file:
     * - punti (.)
     * - trattini bassi (_)
     * - trattini (-)
     * - slash o backslash (/ \)
     * - spazi bianchi (\s+)
     * - qualsiasi carattere di punteggiatura non alfanumerico
     */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("[.\\-_/\\\\\\s]+");

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Il gruppo -1 indica di usare il pattern come delimitatore per separare i token
        Tokenizer tokenizer = new PatternTokenizer(DELIMITER_PATTERN, -1);
        TokenStream filter = new LowerCaseFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
    }
}
