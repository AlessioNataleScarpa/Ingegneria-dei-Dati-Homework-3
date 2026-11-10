package it.uniroma3.hw3.indexing;

import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Implementazione concreta di {@link DocumentBuilder} per Apache Lucene.
 * <p>
 * Applica i pattern:
 * - <b>Creator</b> (GRASP): conosce i dettagli e le dipendenze per instanziare documenti Lucene con i campi appropriati.
 * - <b>Information Expert</b> (GRASP): definisce i nomi canonici dei campi e seleziona il tipo corretto
 *   ({@link TextField} tokenizzato vs {@link StringField} esatto vs {@link StoredField} metadato)
 *   in base a {@link TargetIndex}.
 * </p>
 */
public class LuceneDocumentBuilder implements DocumentBuilder {

    public static final String FIELD_FILENAME = "filename";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_ABSOLUTE_PATH = "absolute_path";
    public static final String FIELD_FILE_SIZE = "file_size";

    @Override
    public Document buildDocument(Path filePath, Path corpusRoot, TargetIndex targetIndex) throws IndexingException {
        Objects.requireNonNull(filePath, "filePath non può essere nullo");
        Objects.requireNonNull(corpusRoot, "corpusRoot non può essere nullo");
        Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IndexingException("Il file specificato non esiste o non è un file regolare: " + filePath);
        }

        try {
            return switch (targetIndex) {
                case FILENAME -> buildFilenameDocument(filePath, corpusRoot);
                case CONTENT -> buildContentDocument(filePath, corpusRoot);
            };
        } catch (IOException e) {
            throw new IndexingException("Errore di I/O durante la creazione del documento Lucene per " + filePath, e);
        }
    }

    private Document buildFilenameDocument(Path filePath, Path corpusRoot) throws IOException {
        Document doc = new Document();

        String filename = filePath.getFileName().toString();
        String relativePath = corpusRoot.relativize(filePath).toString().replace('\\', '/');
        String absolutePath = filePath.toAbsolutePath().toString();
        long fileSize = Files.size(filePath);

        // Nel Filename Index, il nome del file è tokenizzato e ricercabile
        doc.add(new TextField(FIELD_FILENAME, filename, Field.Store.YES));

        // Metadati memorizzati
        doc.add(new StringField(FIELD_PATH, relativePath, Field.Store.YES));
        doc.add(new StoredField(FIELD_ABSOLUTE_PATH, absolutePath));
        doc.add(new StoredField(FIELD_FILE_SIZE, fileSize));

        return doc;
    }

    private Document buildContentDocument(Path filePath, Path corpusRoot) throws IOException {
        Document doc = new Document();

        String filename = filePath.getFileName().toString();
        String relativePath = corpusRoot.relativize(filePath).toString().replace('\\', '/');
        String absolutePath = filePath.toAbsolutePath().toString();
        long fileSize = Files.size(filePath);

        // Streaming del contenuto con gestione resiliente dei charset non-UTF8 (es. Usenet Latin-1)
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(filePath), decoder));
        // TextField(name, Reader) tokenizza il flusso durante l'inversione dell'indice
        doc.add(new TextField(FIELD_CONTENT, reader));

        // Metadati memorizzati per esibizione nei risultati del Searcher
        doc.add(new StoredField(FIELD_FILENAME, filename));
        doc.add(new StringField(FIELD_PATH, relativePath, Field.Store.YES));
        doc.add(new StoredField(FIELD_ABSOLUTE_PATH, absolutePath));
        doc.add(new StoredField(FIELD_FILE_SIZE, fileSize));

        return doc;
    }
}
