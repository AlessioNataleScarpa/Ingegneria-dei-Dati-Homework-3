package it.uniroma3.hw3.searching;

import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.search.Query;

/**
 * Contratto per il servizio di ricerca su indici Apache Lucene.
 * Estende {@link AutoCloseable} per garantire il rilascio deterministico delle risorse I/O.
 * <p>
 * Applica i pattern:
 * - <b>Pure Fabrication</b> (GRASP): disaccoppia la logica di ricerca sia dai modelli di dominio
 *   che dai controller di interfaccia.
 * - <b>Protected Variations</b> (GRASP): incapsula le complessità dell'apertura dei {@code DirectoryReader},
 *   del parsing delle query con analizzatori specifici e del calcolo dei punteggi di rilevanza BM25.
 * </p>
 */
public interface SearchService extends AutoCloseable {

    /**
     * Esegue una ricerca a partire da una query testuale raw, analizzandola con
     * l'analizzatore associato al target index specificato.
     *
     * @param target target su cui effettuare la ricerca (FILENAME o CONTENT)
     * @param rawQuery stringa della query (singolo termine, booleana o frase esatta tra apici)
     * @param topN numero massimo di risultati da recuperare
     * @return {@link SearchResult} immutabile contenente le hit e le statistiche
     * @throws SearchException in caso di errori di I/O, parsing della query o indice non valido
     */
    SearchResult search(TargetIndex target, String rawQuery, int topN) throws SearchException;

    /**
     * Esegue una ricerca a partire da un oggetto {@link Query} Lucene già istanziato.
     *
     * @param target target su cui effettuare la ricerca (FILENAME o CONTENT)
     * @param query query Lucene precompilata
     * @param topN numero massimo di risultati da recuperare
     * @return {@link SearchResult} immutabile contenente le hit e le statistiche
     * @throws SearchException in caso di errori durante la ricerca
     */
    SearchResult searchByQuery(TargetIndex target, Query query, int topN) throws SearchException;

    /**
     * Riapre o aggiorna i DirectoryReader se l'indice su disco è stato modificato (NRT/refresh).
     *
     * @throws SearchException se si verifica un errore durante il refresh
     */
    void refresh() throws SearchException;

    @Override
    void close() throws SearchException;
}
