package it.uniroma3.hw3.exception;

/**
 * Eccezione sollevata durante il parsing della query o l'esecuzione della ricerca.
 */
public class SearchException extends SearchEngineException {

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
