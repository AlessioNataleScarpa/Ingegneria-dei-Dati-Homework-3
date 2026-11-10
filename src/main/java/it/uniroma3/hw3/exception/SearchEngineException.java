package it.uniroma3.hw3.exception;

/**
 * Radice della gerarchia delle eccezioni del dominio per il motore di ricerca.
 * Disaccoppia i layer superiori dai dettagli infrastrutturali di Apache Lucene o del filesystem.
 */
public abstract class SearchEngineException extends Exception {

    public SearchEngineException(String message) {
        super(message);
    }

    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
