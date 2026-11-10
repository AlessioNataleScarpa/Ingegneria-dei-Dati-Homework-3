package it.uniroma3.hw3.cli;

import it.uniroma3.hw3.benchmark.BenchmarkCorpus;
import it.uniroma3.hw3.benchmark.MultiDomainBenchmarkReport;
import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.model.BenchmarkReport;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;

import java.nio.file.Path;
import java.util.Map;

/**
 * Controller di sistema (GRASP Controller / GoF Facade).
 * Rappresenta l'unico punto di ingresso per il presentation layer (CLI o interfacce grafiche/web)
 * coordinando i sottosistemi di indicizzazione, ricerca e benchmark scientifico.
 * Estende {@link AutoCloseable} per garantire il rilascio sicuro e deterministico delle risorse.
 */
public interface SearchEngineFacade extends AutoCloseable {

    /**
     * Esegue l'indicizzazione sdoppiata di una directory di file di testo (.txt).
     *
     * @param corpusDir il percorso della directory contenente i documenti da indicizzare
     * @return mappa contenente le metriche di indicizzazione per ciascun indice (FILENAME e CONTENT)
     * @throws IndexingException in caso di errore durante l'indicizzazione
     */
    Map<TargetIndex, IndexMetrics> indexCorpus(Path corpusDir) throws IndexingException;

    /**
     * Esegue una ricerca sull'indice specificato.
     *
     * @param target l'indice bersaglio (FILENAME o CONTENT)
     * @param queryString la query testuale (termine singolo, booleana AND/OR/NOT o frase esatta "...")
     * @param maxHits numero massimo di risultati rilevanti da restituire
     * @return i risultati della ricerca con metriche di latenza e snippet evidenziati
     * @throws SearchException in caso di errore durante la ricerca
     */
    SearchResult search(TargetIndex target, String queryString, int maxHits) throws SearchException;

    /**
     * Esegue la ricerca con il default di 10 hit massimi.
     *
     * @param target l'indice bersaglio (FILENAME o CONTENT)
     * @param queryString la query testuale
     * @return i risultati della ricerca
     * @throws SearchException in caso di errore durante la ricerca
     */
    default SearchResult search(TargetIndex target, String queryString) throws SearchException {
        return search(target, queryString, 10);
    }

    /**
     * Esegue il benchmark accademico completo per uno specifico corpus standard della letteratura IR.
     *
     * @param corpus il corpus da valutare (es. CISI, CRANFIELD, NEWSGROUPS)
     * @return il report dettagliato con tempi di indicizzazione, spazio disco e latenze delle query
     * @throws BenchmarkException in caso di errore durante il benchmark
     */
    BenchmarkReport runBenchmark(BenchmarkCorpus corpus) throws BenchmarkException;

    /**
     * Esegue la suite completa di benchmark cross-domain su tutti i corpus standard della letteratura IR.
     *
     * @return il report comparativo multi-dominio
     * @throws BenchmarkException in caso di errore durante il benchmark
     */
    MultiDomainBenchmarkReport runAllBenchmarks() throws BenchmarkException;

    /**
     * Chiude le risorse sottostanti (es. DirectoryReader e indici aperti).
     */
    @Override
    void close();
}
