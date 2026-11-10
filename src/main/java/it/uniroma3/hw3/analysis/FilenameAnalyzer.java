package it.uniroma3.hw3.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.pattern.PatternTokenizer;

import java.util.regex.Pattern;

/**
 * Custom Analyzer dedicato all'indicizzazione e ricerca dei nomi di file (Filename Index - Indice A).
 * <p>
 * Specifiche di elaborazione:
 * 1. Tokenizzazione tramite {@link PatternTokenizer} basata sull'espressione regolare {@code [.\\-_/\\\\\\s]+},
 *    che separa i termini in corrispondenza di punti, trattini, trattini bassi, slash/backslash e spazi.
 * 2. Normalizzazione tramite {@link LowerCaseFilter} per garantire ricerche case-insensitive.
 * 3. Assenza intenzionale di stop-words e stemming per preservare codici, estensioni, acronimi
 *    e frammenti alfanumerici caratteristici dei nomi di file.
 * </p>
 */
public class FilenameAnalyzer extends Analyzer {

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("[.\\-_/\\\\\\s]+");

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new PatternTokenizer(DELIMITER_PATTERN, -1);
        TokenStream filter = new LowerCaseFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
    }
}
