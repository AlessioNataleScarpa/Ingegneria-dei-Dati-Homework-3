package it.uniroma3.hw3.indexing;

import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.document.Document;

import java.nio.file.Path;

/**
 * Contratto per la trasformazione di un file del filesystem in un Document Lucene.
 * Pattern GRASP: Creator & Information Expert.
 */
public interface DocumentBuilder {

    /**
     * Costruisce un Document Lucene a partire da un file sorgente e dal target prescelto.
     *
     * @param filePath    il percorso al file fisico .txt
     * @param corpusRoot  la radice del corpus (usata per calcolare il path relativo)
     * @param targetIndex l'indice bersaglio (FILENAME o CONTENT)
     * @return il Document Lucene configurato con i relativi campi
     * @throws IndexingException in caso di errore di I/O o accesso al file
     */
    Document buildDocument(Path filePath, Path corpusRoot, TargetIndex targetIndex) throws IndexingException;
}
