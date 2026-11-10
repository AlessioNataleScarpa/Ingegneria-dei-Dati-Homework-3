package it.uniroma3.ingegneriadeidati.hw3.indexer;

import it.uniroma3.ingegneriadeidati.hw3.analyzer.ContentAnalyzer;
import it.uniroma3.ingegneriadeidati.hw3.analyzer.FilenameAnalyzer;
import it.uniroma3.ingegneriadeidati.hw3.model.IndexStats;
import it.uniroma3.ingegneriadeidati.hw3.util.FileUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestore dell'indicizzazione per l'Homework 3.
 * Supporta la costruzione separata o congiunta di:
 * 1. Indice A: Nomi dei file (Filename Index)
 * 2. Indice B: Contenuto dei file (Content Index)
 */
public class DocumentIndexer {

    // Nomi standard dei campi nei documenti Lucene
    public static final String FIELD_FILENAME = "filename";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_ABSOLUTE_PATH = "absolute_path";
    public static final String FIELD_SIZE = "file_size";

    /**
     * Indicizza una directory di file di testo sia per nome sia per contenuto.
     *
     * @param corpusDir       Directory sorgente contenente file .txt
     * @param filenameIndexDir Directory di destinazione per l'indice del nome
     * @param contentIndexDir  Directory di destinazione per l'indice del contenuto
     * @return Statistiche di indicizzazione per entrambi gli indici
     */
    public List<IndexStats> indexCorpus(Path corpusDir, Path filenameIndexDir, Path contentIndexDir) throws IOException {
        List<IndexStats> stats = new ArrayList<>();
        stats.add(buildFilenameIndex(corpusDir, filenameIndexDir));
        stats.add(buildContentIndex(corpusDir, contentIndexDir));
        return stats;
    }

    /**
     * Costruisce l'Indice A (Filename Index).
     */
    public IndexStats buildFilenameIndex(Path corpusDir, Path indexPath) throws IOException {
        if (!Files.exists(corpusDir) || !Files.isDirectory(corpusDir)) {
            throw new IllegalArgumentException("La cartella del corpus non esiste o non è una cartella: " + corpusDir);
        }

        FileUtils.deleteDirectoryRecursively(indexPath);
        Files.createDirectories(indexPath);

        List<Path> files = collectTextFiles(corpusDir);
        long totalCorpusBytes = calculateCorpusBytes(files);

        long startTime = System.currentTimeMillis();

        try (Directory dir = FSDirectory.open(indexPath);
             FilenameAnalyzer analyzer = new FilenameAnalyzer()) {

            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // Ottimizzazione RAM buffer per indicizzazione veloce
            iwc.setRAMBufferSizeMB(128.0);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (Path file : files) {
                    Document doc = new Document();
                    String filename = file.getFileName().toString();
                    String relativePath = corpusDir.relativize(file).toString().replace('\\', '/');
                    String absolutePath = file.toAbsolutePath().toString();
                    long size = Files.size(file);

                    // Campo tokenizzato per la ricerca testuale sul nome
                    doc.add(new TextField(FIELD_FILENAME, filename, Field.Store.YES));

                    // Metadati per visualizzazione e riferimento
                    doc.add(new StringField(FIELD_PATH, relativePath, Field.Store.YES));
                    doc.add(new StoredField(FIELD_ABSOLUTE_PATH, absolutePath));
                    doc.add(new StoredField(FIELD_SIZE, size));

                    writer.addDocument(doc);
                }
                writer.commit();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        long indexDiskBytes = FileUtils.calculateDirectorySize(indexPath);

        return new IndexStats("Filename Index", files.size(), duration, indexDiskBytes, totalCorpusBytes);
    }

    /**
     * Costruisce l'Indice B (Content Index).
     */
    public IndexStats buildContentIndex(Path corpusDir, Path indexPath) throws IOException {
        if (!Files.exists(corpusDir) || !Files.isDirectory(corpusDir)) {
            throw new IllegalArgumentException("La cartella del corpus non esiste o non è una cartella: " + corpusDir);
        }

        FileUtils.deleteDirectoryRecursively(indexPath);
        Files.createDirectories(indexPath);

        List<Path> files = collectTextFiles(corpusDir);
        long totalCorpusBytes = calculateCorpusBytes(files);

        long startTime = System.currentTimeMillis();

        try (Directory dir = FSDirectory.open(indexPath);
             ContentAnalyzer analyzer = new ContentAnalyzer()) {

            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            iwc.setRAMBufferSizeMB(256.0);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (Path file : files) {
                    Document doc = new Document();
                    String filename = file.getFileName().toString();
                    String relativePath = corpusDir.relativize(file).toString().replace('\\', '/');
                    String absolutePath = file.toAbsolutePath().toString();
                    long size = Files.size(file);

                    // Lettura in streaming del file per indicizzazione ad alte prestazioni
                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        // TextField con Reader (non memorizza tutto il testo nell'indice ma lo tokenizza)
                        doc.add(new TextField(FIELD_CONTENT, reader));

                        // Memorizziamo il nome file e percorsi come campi memorizzati per esibizione nei risultati
                        doc.add(new StoredField(FIELD_FILENAME, filename));
                        doc.add(new StringField(FIELD_PATH, relativePath, Field.Store.YES));
                        doc.add(new StoredField(FIELD_ABSOLUTE_PATH, absolutePath));
                        doc.add(new StoredField(FIELD_SIZE, size));

                        writer.addDocument(doc);
                    } catch (IOException e) {
                        System.err.println("Avviso: impossibile leggere il file " + file + ": " + e.getMessage());
                    }
                }
                writer.commit();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        long indexDiskBytes = FileUtils.calculateDirectorySize(indexPath);

        return new IndexStats("Content Index", files.size(), duration, indexDiskBytes, totalCorpusBytes);
    }

    /**
     * Raccoglie ricorsivamente tutti i file con estensione .txt o documenti di testo.
     */
    public static List<Path> collectTextFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
                  .forEach(files::add);
        }
        return files;
    }

    private static long calculateCorpusBytes(List<Path> files) {
        long sum = 0L;
        for (Path f : files) {
            try {
                sum += Files.size(f);
            } catch (IOException ignored) {}
        }
        return sum;
    }
}
