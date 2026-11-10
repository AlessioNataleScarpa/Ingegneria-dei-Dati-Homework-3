package it.uniroma3.hw3.searching;

import it.uniroma3.hw3.analysis.AnalyzerStrategy;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.indexing.LuceneDocumentBuilder;
import it.uniroma3.hw3.model.SearchHit;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementazione concreta di {@link SearchService} per Apache Lucene.
 * <p>
 * Applica i pattern:
 * - <b>Pure Fabrication</b> (GRASP): incapsula la gestione del ciclo di vita dei lettori e degli esecutori di query.
 * - <b>Information Expert</b> (GRASP): conosce la mappatura dei campi Lucene e recupera i metadati memorizzati.
 * - <b>Low Coupling</b> (GRASP): riceve le dipendenze da {@link AnalyzerStrategy} e percorsi via costruttore.
 * </p>
 */
public class LuceneSearchService implements SearchService {

    private static final int MAX_SNIPPET_LENGTH = 160;

    private final Path filenameIndexPath;
    private final Path contentIndexPath;
    private final AnalyzerStrategy analyzerStrategy;

    private Directory filenameDirectory;
    private Directory contentDirectory;
    private DirectoryReader filenameReader;
    private DirectoryReader contentReader;
    private IndexSearcher filenameSearcher;
    private IndexSearcher contentSearcher;

    private boolean closed = false;

    public LuceneSearchService(Path filenameIndexPath, Path contentIndexPath, AnalyzerStrategy analyzerStrategy) {
        this.filenameIndexPath = Objects.requireNonNull(filenameIndexPath, "filenameIndexPath non può essere nullo");
        this.contentIndexPath = Objects.requireNonNull(contentIndexPath, "contentIndexPath non può essere nullo");
        this.analyzerStrategy = Objects.requireNonNull(analyzerStrategy, "analyzerStrategy non può essere nullo");
    }

    @Override
    public SearchResult search(TargetIndex target, String rawQuery, int topN) throws SearchException {
        checkClosed();
        Objects.requireNonNull(target, "target non può essere nullo");
        Objects.requireNonNull(rawQuery, "rawQuery non può essere nullo");
        if (topN <= 0) {
            throw new IllegalArgumentException("topN deve essere maggiore di zero: " + topN);
        }

        String trimmedQuery = rawQuery.trim();
        if (trimmedQuery.isEmpty()) {
            return new SearchResult(rawQuery, target, 0, 0, List.of());
        }

        String searchField = switch (target) {
            case FILENAME -> LuceneDocumentBuilder.FIELD_FILENAME;
            case CONTENT -> LuceneDocumentBuilder.FIELD_CONTENT;
        };

        Analyzer analyzer = analyzerStrategy.getAnalyzer(target);
        QueryParser parser = new QueryParser(searchField, analyzer);

        Query query;
        try {
            query = parser.parse(trimmedQuery);
        } catch (ParseException e) {
            throw new SearchException("Sintassi della query non valida: '" + rawQuery + "'", e);
        }

        return executeSearch(target, query, rawQuery, topN);
    }

    @Override
    public SearchResult searchByQuery(TargetIndex target, Query query, int topN) throws SearchException {
        checkClosed();
        Objects.requireNonNull(target, "target non può essere nullo");
        Objects.requireNonNull(query, "query non può essere nullo");
        if (topN <= 0) {
            throw new IllegalArgumentException("topN deve essere maggiore di zero: " + topN);
        }

        return executeSearch(target, query, query.toString(), topN);
    }

    private SearchResult executeSearch(TargetIndex target, Query query, String originalQueryText, int topN) throws SearchException {
        IndexSearcher searcher = getSearcher(target);

        long start = System.currentTimeMillis();
        TopDocs topDocs;
        try {
            topDocs = searcher.search(query, topN);
        } catch (IOException e) {
            throw new SearchException("Errore I/O durante l'esecuzione della ricerca per '" + originalQueryText + "'", e);
        }
        long durationMs = System.currentTimeMillis() - start;

        long totalHits = topDocs.totalHits.value;
        List<SearchHit> hits = new ArrayList<>(topDocs.scoreDocs.length);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            hits.add(buildSearchHit(searcher, scoreDoc, target, originalQueryText));
        }

        return new SearchResult(originalQueryText, target, totalHits, durationMs, hits);
    }

    private SearchHit buildSearchHit(IndexSearcher searcher, ScoreDoc scoreDoc, TargetIndex target, String queryText) throws SearchException {
        try {
            Document doc = searcher.storedFields().document(scoreDoc.doc);

            String filename = doc.get(LuceneDocumentBuilder.FIELD_FILENAME);
            String relativePath = doc.get(LuceneDocumentBuilder.FIELD_PATH);
            String absolutePath = doc.get(LuceneDocumentBuilder.FIELD_ABSOLUTE_PATH);
            String fileSizeStr = doc.get(LuceneDocumentBuilder.FIELD_FILE_SIZE);

            long fileSize = 0;
            if (fileSizeStr != null) {
                try {
                    fileSize = Long.parseLong(fileSizeStr);
                } catch (NumberFormatException ignored) {
                }
            }

            String snippet = "";
            if (target == TargetIndex.CONTENT && absolutePath != null) {
                snippet = extractSnippet(Path.of(absolutePath), queryText);
            }

            return new SearchHit(
                    scoreDoc.doc,
                    scoreDoc.score,
                    filename != null ? filename : "N/D",
                    relativePath != null ? relativePath : "N/D",
                    absolutePath,
                    fileSize,
                    snippet
            );
        } catch (IOException e) {
            throw new SearchException("Impossibile recuperare i dettagli per il documento con ID: " + scoreDoc.doc, e);
        }
    }

    private String extractSnippet(Path filePath, String queryText) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return "";
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            StringBuilder snippetBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String cleanLine = line.trim();
                if (!cleanLine.isEmpty()) {
                    if (snippetBuilder.length() > 0) {
                        snippetBuilder.append(" ");
                    }
                    snippetBuilder.append(cleanLine);
                    if (snippetBuilder.length() >= MAX_SNIPPET_LENGTH) {
                        break;
                    }
                }
            }

            if (snippetBuilder.length() > MAX_SNIPPET_LENGTH) {
                return snippetBuilder.substring(0, MAX_SNIPPET_LENGTH) + "...";
            }
            return snippetBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private synchronized IndexSearcher getSearcher(TargetIndex target) throws SearchException {
        checkClosed();
        try {
            return switch (target) {
                case FILENAME -> {
                    if (filenameReader == null) {
                        if (!Files.exists(filenameIndexPath)) {
                            throw new SearchException("Indice dei nomi file non presente nel percorso: " + filenameIndexPath);
                        }
                        filenameDirectory = FSDirectory.open(filenameIndexPath);
                        filenameReader = DirectoryReader.open(filenameDirectory);
                        filenameSearcher = new IndexSearcher(filenameReader);
                    }
                    yield filenameSearcher;
                }
                case CONTENT -> {
                    if (contentReader == null) {
                        if (!Files.exists(contentIndexPath)) {
                            throw new SearchException("Indice dei contenuti non presente nel percorso: " + contentIndexPath);
                        }
                        contentDirectory = FSDirectory.open(contentIndexPath);
                        contentReader = DirectoryReader.open(contentDirectory);
                        contentSearcher = new IndexSearcher(contentReader);
                    }
                    yield contentSearcher;
                }
            };
        } catch (IOException e) {
            throw new SearchException("Impossibile aprire l'indice per " + target + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void refresh() throws SearchException {
        checkClosed();
        try {
            if (filenameReader != null) {
                DirectoryReader newReader = DirectoryReader.openIfChanged(filenameReader);
                if (newReader != null) {
                    filenameReader.close();
                    filenameReader = newReader;
                    filenameSearcher = new IndexSearcher(filenameReader);
                }
            }
            if (contentReader != null) {
                DirectoryReader newReader = DirectoryReader.openIfChanged(contentReader);
                if (newReader != null) {
                    contentReader.close();
                    contentReader = newReader;
                    contentSearcher = new IndexSearcher(contentReader);
                }
            }
        } catch (IOException e) {
            throw new SearchException("Errore durante l'aggiornamento dei lettori dell'indice", e);
        }
    }

    @Override
    public synchronized void close() throws SearchException {
        if (closed) {
            return;
        }
        closed = true;

        List<Exception> suppressed = new ArrayList<>();

        if (filenameReader != null) {
            try {
                filenameReader.close();
            } catch (IOException e) {
                suppressed.add(e);
            }
        }
        if (filenameDirectory != null) {
            try {
                filenameDirectory.close();
            } catch (IOException e) {
                suppressed.add(e);
            }
        }

        if (contentReader != null) {
            try {
                contentReader.close();
            } catch (IOException e) {
                suppressed.add(e);
            }
        }
        if (contentDirectory != null) {
            try {
                contentDirectory.close();
            } catch (IOException e) {
                suppressed.add(e);
            }
        }

        if (!suppressed.isEmpty()) {
            SearchException ex = new SearchException("Errore durante la chiusura delle risorse dell'indice");
            suppressed.forEach(ex::addSuppressed);
            throw ex;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("LuceneSearchService è già stato chiuso");
        }
    }
}
