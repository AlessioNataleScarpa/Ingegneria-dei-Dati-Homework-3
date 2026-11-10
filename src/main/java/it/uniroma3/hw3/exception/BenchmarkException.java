package it.uniroma3.hw3.exception;

/**
 * Eccezione sollevata durante l'esecuzione del benchmark sperimentale o della generazione del dataset.
 */
public class BenchmarkException extends SearchEngineException {

    public BenchmarkException(String message) {
        super(message);
    }

    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }
}
