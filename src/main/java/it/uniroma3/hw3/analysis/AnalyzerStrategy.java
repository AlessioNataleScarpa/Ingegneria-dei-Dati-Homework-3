package it.uniroma3.hw3.analysis;

import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.analysis.Analyzer;

/**
 * Contratto Strategy (GoF) per la fornitura degli Analyzer Lucene in funzione dell'indice target.
 * Estende {@link AutoCloseable} per garantire il pattern RAII e la corretta deallocazione
 * delle risorse native e dei buffer dei TokenStream associati.
 */
public interface AnalyzerStrategy extends AutoCloseable {

    /**
     * Restituisce l'istanza appropriata di {@link Analyzer} configurata per il {@link TargetIndex} richiesto.
     *
     * @param target L'indice bersaglio (FILENAME o CONTENT)
     * @return L'Analyzer dedicato
     */
    Analyzer getAnalyzer(TargetIndex target);

    @Override
    void close();
}
