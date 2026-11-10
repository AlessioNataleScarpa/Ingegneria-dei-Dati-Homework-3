package it.uniroma3.ingegneriadeidati.hw3.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.tartarus.snowball.ext.ItalianStemmer;

/**
 * Custom Analyzer per l'indicizzazione del contenuto dei documenti testuali (Content Index).
 * <p>
 * Pipeline di analisi del linguaggio naturale (Italiano / Standard):
 * 1. StandardTokenizer: tokenizzazione conforme allo standard Unicode (spezza parole, punteggiatura).
 * 2. LowerCaseFilter: conversione di tutti i caratteri in minuscolo.
 * 3. ASCIIFoldingFilter: rimozione e normalizzazione degli accenti (es. "è" -> "e", "perché" -> "perche").
 * 4. StopFilter: eliminazione delle stop-words italiane (e inglesi per termini comuni).
 * 5. ItalianStemmer (SnowballFilter): riduzione morfologica dei termini alla radice lessicale.
 * </p>
 */
public class ContentAnalyzer extends Analyzer {

    private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
            new CharArraySet(
                    java.util.Arrays.asList(
                            "c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell",
                            "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"
                    ),
                    true
            )
    );

    private final CharArraySet stopWords;

    /**
     * Costruttore di default: unisce le stop-words italiane standard con quelle inglesi
     * per garantire robustezza su corpus misti tecnico-scientifici.
     */
    public ContentAnalyzer() {
        CharArraySet combined = new CharArraySet(ItalianAnalyzer.getDefaultStopSet(), true);
        combined.addAll(EnglishAnalyzer.getDefaultStopSet());
        this.stopWords = CharArraySet.unmodifiableSet(combined);
    }

    /**
     * Costruttore con stop-words personalizzate.
     *
     * @param stopWords Insieme di parole vuote (stop-words).
     */
    public ContentAnalyzer(CharArraySet stopWords) {
        this.stopWords = stopWords != null ? stopWords : CharArraySet.EMPTY_SET;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();

        // 1. Elisione tipica (l', un', all', dell', ecc.) usando il set di articoli italiani
        TokenStream stream = new ElisionFilter(tokenizer, DEFAULT_ARTICLES);

        // 2. Lowercase
        stream = new LowerCaseFilter(stream);

        // 3. ASCII folding (gestione accenti)
        stream = new ASCIIFoldingFilter(stream);

        // 4. Stop words filter
        stream = new StopFilter(stream, this.stopWords);

        // 5. Stemming con ItalianStemmer (Snowball)
        stream = new SnowballFilter(stream, new ItalianStemmer());

        return new TokenStreamComponents(tokenizer, stream);
    }
}
