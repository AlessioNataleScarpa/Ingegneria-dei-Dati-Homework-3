package it.uniroma3.hw3.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.tartarus.snowball.ext.ItalianStemmer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Custom Analyzer dedicato all'indicizzazione e ricerca del contenuto testuale (Content Index - Indice B).
 * <p>
 * Pipeline di elaborazione per linguaggio naturale (Italiano / Documenti tecnici bilingue):
 * 1. Tokenizzazione standard conforme a Unicode tramite {@link StandardTokenizer}.
 * 2. Gestione delle elisioni tipiche dell'italiano (l', un', dall', dell', ecc.) tramite {@link ElisionFilter}.
 * 3. Normalizzazione minuscola tramite {@link LowerCaseFilter}.
 * 4. Rimozione degli accenti e mappatura verso caratteri ASCII tramite {@link ASCIIFoldingFilter}.
 * 5. Eliminazione delle stop-words tramite {@link StopFilter} con un set bilingue (italiano + inglese).
 * 6. Stemming morfologico tramite {@link SnowballFilter} e {@link ItalianStemmer}.
 * </p>
 */
public class ContentAnalyzer extends Analyzer {

    private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
            new CharArraySet(
                    Arrays.asList(
                            "c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell",
                            "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"
                    ),
                    true
            )
    );

    private final CharArraySet stopWords;

    /**
     * Costruisce il ContentAnalyzer utilizzando il set di stop-words predefinito (italiano + inglese).
     */
    public ContentAnalyzer() {
        this(createDefaultStopSet());
    }

    /**
     * Costruttore flessibile che consente l'iniezione di un set personalizzato di stop-words.
     */
    public ContentAnalyzer(CharArraySet stopWords) {
        this.stopWords = CharArraySet.unmodifiableSet(Objects.requireNonNull(stopWords, "stopWords non può essere nullo"));
    }

    public static CharArraySet createDefaultStopSet() {
        CharArraySet combined = new CharArraySet(ItalianAnalyzer.getDefaultStopSet(), true);
        combined.addAll(EnglishAnalyzer.getDefaultStopSet());
        return CharArraySet.unmodifiableSet(combined);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();

        TokenStream filter = new ElisionFilter(tokenizer, DEFAULT_ARTICLES);
        filter = new LowerCaseFilter(filter);
        filter = new ASCIIFoldingFilter(filter);
        filter = new StopFilter(filter, this.stopWords);
        filter = new SnowballFilter(filter, new ItalianStemmer());

        return new TokenStreamComponents(tokenizer, filter);
    }
}
