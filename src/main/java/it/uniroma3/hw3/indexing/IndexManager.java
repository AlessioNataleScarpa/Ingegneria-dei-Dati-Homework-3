package it.uniroma3.hw3.indexing;

import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.TargetIndex;

import java.nio.file.Path;
import java.util.List;

/**
 * Servizio applicativo per la gestione dell'indicizzazione fisica del corpus.
 * Pattern GRASP: High Cohesion, Protected Variations.
 */
public interface IndexManager {

    /**
     * Indicizza il corpus specificato su un singolo indice fisico.
     *
     * @param corpusPath  cartella contenente i file .txt
     * @param indexPath   cartella di destinazione dell'indice su disco
     * @param targetIndex indice da costruire (FILENAME o CONTENT)
     * @return metriche dettagliate sull'indicizzazione eseguita
     * @throws IndexingException in caso di fallimento della procedura
     */
    IndexMetrics index(Path corpusPath, Path indexPath, TargetIndex targetIndex) throws IndexingException;

    /**
     * Costruisce entrambi gli indici fisici (Filename e Content) in sequenza.
     *
     * @param corpusPath        cartella contenente i file .txt
     * @param filenameIndexPath cartella di destinazione per l'indice dei nomi
     * @param contentIndexPath  cartella di destinazione per l'indice dei contenuti
     * @return lista contenente le metriche di entrambi gli indici
     * @throws IndexingException in caso di errore
     */
    List<IndexMetrics> indexAll(Path corpusPath, Path filenameIndexPath, Path contentIndexPath) throws IndexingException;
}
