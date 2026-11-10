package it.uniroma3.hw3.analysis;

import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.analysis.Analyzer;

import java.util.Objects;

/**
 * Provider e Registry predefinito per le strategie di analisi lessicale.
 * <p>
 * Applica i pattern:
 * - <b>Factory / Registry</b> (GoF): centralizza la creazione e gestione delle istanze di Analyzer.
 * - <b>Strategy</b> (GoF): fornisce la specifica implementazione di Analyzer in base al {@link TargetIndex}.
 * - <b>RAII</b>: alla chiusura rilascia tutte le risorse associate agli analizzatori gestiti.
 * </p>
 */
public class DefaultAnalyzerProvider implements AnalyzerStrategy {

    private final Analyzer filenameAnalyzer;
    private final Analyzer contentAnalyzer;

    public DefaultAnalyzerProvider() {
        this(new FilenameAnalyzer(), new ContentAnalyzer());
    }

    public DefaultAnalyzerProvider(Analyzer filenameAnalyzer, Analyzer contentAnalyzer) {
        this.filenameAnalyzer = Objects.requireNonNull(filenameAnalyzer, "filenameAnalyzer non può essere nullo");
        this.contentAnalyzer = Objects.requireNonNull(contentAnalyzer, "contentAnalyzer non può essere nullo");
    }

    @Override
    public Analyzer getAnalyzer(TargetIndex target) {
        Objects.requireNonNull(target, "TargetIndex non può essere nullo");
        return switch (target) {
            case FILENAME -> filenameAnalyzer;
            case CONTENT -> contentAnalyzer;
        };
    }

    @Override
    public void close() {
        try {
            filenameAnalyzer.close();
        } finally {
            contentAnalyzer.close();
        }
    }
}
