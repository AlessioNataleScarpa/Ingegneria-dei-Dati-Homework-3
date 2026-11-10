package it.uniroma3.hw3.exception;

/**
 * Eccezione sollevata durante la scansione o l'indicizzazione dei documenti.
 */
public class IndexingException extends SearchEngineException {

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
