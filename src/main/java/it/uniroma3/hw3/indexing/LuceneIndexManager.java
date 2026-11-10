package it.uniroma3.hw3.indexing;

import it.uniroma3.hw3.analysis.AnalyzerStrategy;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Implementazione concreta di {@link IndexManager} basata su Apache Lucene.
 * <p>
 * Applica i pattern:
 * - <b>Strategy & Protected Variations</b> (GoF / GRASP): la strategia di analisi (tokenizer e filtri)
 *   è iniettata tramite {@link AnalyzerStrategy}.
 * - <b>Creator</b> (GRASP): delega la costruzione del {@link Document} Lucene a {@link DocumentBuilder}.
 * - <b>RAII / Try-with-resources</b>: garantisce il corretto commit e rilascio delle risorse di {@link IndexWriter} e {@link Directory}.
 * - <b>High Cohesion</b>: focalizzato esclusivamente sul lifecycle dell'indicizzazione fisica e calcolo metriche.
 * </p>
 */
public class LuceneIndexManager implements IndexManager {

    private static final double DEFAULT_RAM_BUFFER_MB = 128.0;

    private final AnalyzerStrategy analyzerStrategy;
    private final DocumentBuilder documentBuilder;
    private final double ramBufferSizeMB;

    public LuceneIndexManager(AnalyzerStrategy analyzerStrategy) {
        this(analyzerStrategy, new LuceneDocumentBuilder(), DEFAULT_RAM_BUFFER_MB);
    }

    public LuceneIndexManager(AnalyzerStrategy analyzerStrategy, DocumentBuilder documentBuilder, double ramBufferSizeMB) {
        this.analyzerStrategy = Objects.requireNonNull(analyzerStrategy, "analyzerStrategy non può essere nullo");
        this.documentBuilder = Objects.requireNonNull(documentBuilder, "documentBuilder non può essere nullo");
        if (ramBufferSizeMB <= 0) {
            throw new IllegalArgumentException("ramBufferSizeMB deve essere maggiore di zero: " + ramBufferSizeMB);
        }
        this.ramBufferSizeMB = ramBufferSizeMB;
    }

    @Override
    public IndexMetrics index(Path corpusPath, Path indexPath, TargetIndex targetIndex) throws IndexingException {
        Objects.requireNonNull(corpusPath, "corpusPath non può essere nullo");
        Objects.requireNonNull(indexPath, "indexPath non può essere nullo");
        Objects.requireNonNull(targetIndex, "targetIndex non può essere nullo");

        if (!Files.exists(corpusPath) || !Files.isDirectory(corpusPath)) {
            throw new IndexingException("La directory del corpus non esiste o non è una directory: " + corpusPath);
        }

        List<Path> files = collectFiles(corpusPath);
        long corpusSizeBytes = calculateCorpusSize(files);

        // Pulizia e ricreazione della directory indice per garantire isolamento
        cleanDirectory(indexPath);

        long startTime = System.currentTimeMillis();

        Analyzer analyzer = analyzerStrategy.getAnalyzer(targetIndex);

        try (Directory directory = FSDirectory.open(indexPath)) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            config.setRAMBufferSizeMB(ramBufferSizeMB);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (Path file : files) {
                    Document doc = documentBuilder.buildDocument(file, corpusPath, targetIndex);
                    writer.addDocument(doc);
                }
                writer.commit();
            }
        } catch (IOException e) {
            throw new IndexingException("Errore durante l'indicizzazione su disco per l'indice " + targetIndex + ": " + e.getMessage(), e);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        long diskSizeBytes = calculateDirectorySize(indexPath);

        return new IndexMetrics(
                targetIndex,
                files.size(),
                durationMs,
                diskSizeBytes,
                corpusSizeBytes
        );
    }

    @Override
    public List<IndexMetrics> indexAll(Path corpusPath, Path filenameIndexPath, Path contentIndexPath) throws IndexingException {
        IndexMetrics filenameMetrics = index(corpusPath, filenameIndexPath, TargetIndex.FILENAME);
        IndexMetrics contentMetrics = index(corpusPath, contentIndexPath, TargetIndex.CONTENT);
        return List.of(filenameMetrics, contentMetrics);
    }

    /**
     * Scansiona ricorsivamente la cartella del corpus raccogliendo solo i file con estensione .txt.
     */
    private List<Path> collectFiles(Path rootDir) throws IndexingException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
                .forEach(files::add);
        } catch (IOException e) {
            throw new IndexingException("Errore durante la scansione ricorsiva della cartella del corpus: " + rootDir, e);
        }
        return files;
    }

    private long calculateCorpusSize(List<Path> files) {
        long sum = 0L;
        for (Path f : files) {
            try {
                sum += Files.size(f);
            } catch (IOException ignored) {
            }
        }
        return sum;
    }

    private void cleanDirectory(Path dir) throws IndexingException {
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                          .forEach(path -> {
                              try {
                                  Files.deleteIfExists(path);
                              } catch (IOException ignored) {
                              }
                          });
                }
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IndexingException("Impossibile preparare/pulire la directory di destinazione dell'indice: " + dir, e);
        }
    }

    private long calculateDirectorySize(Path dir) {
        if (!Files.exists(dir)) {
            return 0L;
        }
        long[] total = new long[]{0L};
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    total[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
        return total[0];
    }
}
