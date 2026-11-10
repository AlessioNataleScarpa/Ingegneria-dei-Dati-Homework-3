package it.uniroma3.hw3.model;

import java.util.Objects;

/**
 * Rappresenta il bersaglio dell'indice fisico (Indice A vs Indice B).
 * Applica il pattern Information Expert incapsulando il nome del campo di default
 * di Lucene e la logica di parsing delle stringhe utente da CLI.
 */
public enum TargetIndex {
    FILENAME("filename", "Indice dei Nomi File"),
    CONTENT("content", "Indice dei Contenuti");

    private final String defaultFieldName;
    private final String humanReadableName;

    TargetIndex(String defaultFieldName, String humanReadableName) {
        this.defaultFieldName = defaultFieldName;
        this.humanReadableName = humanReadableName;
    }

    public String getDefaultFieldName() {
        return defaultFieldName;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    /**
     * Mappa un token arbitrario inserito dall'utente verso l'enum corrispondente.
     * Supporta prefissi italiani ("nome", "contenuto") e inglesi ("file", "filename", "content").
     */
    public static TargetIndex fromString(String rawToken) {
        Objects.requireNonNull(rawToken, "Il token di selezione indice non può essere nullo");
        String normalized = rawToken.trim().toLowerCase();
        if (normalized.startsWith("nom") || normalized.startsWith("file")) {
            return FILENAME;
        } else if (normalized.startsWith("cont")) {
            return CONTENT;
        }
        throw new IllegalArgumentException(
                "Identificatore indice non riconosciuto: '" + rawToken + "'. Usa 'nome' oppure 'contenuto'."
        );
    }
}
