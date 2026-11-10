package it.uniroma3.ingegneriadeidati.hw3.searcher;

import it.uniroma3.ingegneriadeidati.hw3.analyzer.ContentAnalyzer;
import it.uniroma3.ingegneriadeidati.hw3.analyzer.FilenameAnalyzer;
import it.uniroma3.ingegneriadeidati.hw3.indexer.DocumentIndexer;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchHit;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestore dell'interrogazione (Searcher) per Apache Lucene.
 * Permette l'esecuzione di:
 * - Query via QueryParser (supporta booleani AND, OR, NOT, frasi esatte con virgolette "...", e raggruppamenti)
 * - TermQuery programmatiche
 * - PhraseQuery programmatiche
 * - BooleanQuery programmatiche
 */
public class DocumentSearcher implements Closeable {

    public enum TargetIndex {
        FILENAME,
        CONTENT
    }

    private final Path filenameIndexPath;
    private final Path contentIndexPath;

    private IndexReader filenameReader;
    private IndexSearcher filenameSearcher;
    private FilenameAnalyzer filenameAnalyzer;

    private IndexReader contentReader;
    private IndexSearcher contentSearcher;
    private ContentAnalyzer contentAnalyzer;

    public DocumentSearcher(Path filenameIndexPath, Path contentIndexPath) throws IOException {
        this.filenameIndexPath = filenameIndexPath;
        this.contentIndexPath = contentIndexPath;
        initReaders();
    }

    private synchronized void initReaders() throws IOException {
        if (Files.exists(filenameIndexPath)) {
            this.filenameReader = DirectoryReader.open(FSDirectory.open(filenameIndexPath));
            this.filenameSearcher = new IndexSearcher(filenameReader);
            this.filenameAnalyzer = new FilenameAnalyzer();
        }

        if (Files.exists(contentIndexPath)) {
            this.contentReader = DirectoryReader.open(FSDirectory.open(contentIndexPath));
            this.contentSearcher = new IndexSearcher(contentReader);
            this.contentAnalyzer = new ContentAnalyzer();
        }
    }

    /**
     * Ricarica gli IndexReader qualora gli indici siano stati aggiornati.
     */
    public synchronized void reopen() throws IOException {
        close();
        initReaders();
    }

    /**
     * Esegue una ricerca testuale elaborando la stringa di query attraverso il relativo QueryParser.
     *
     * @param target  Indice target (FILENAME o CONTENT)
     * @param queryText Testo della query (es. "progetto", "ingegneria AND dati", "\"database relazionale\"")
     * @param topN    Numero massimo di risultati da restituire
     */
    public SearchResult search(TargetIndex target, String queryText, int topN) throws IOException, ParseException {
        IndexSearcher searcher;
        Analyzer analyzer;
        String defaultField;
        String indexLabel;

        if (target == TargetIndex.FILENAME) {
            if (filenameSearcher == null) throw new IllegalStateException("L'indice dei nomi file non è aperto.");
            searcher = filenameSearcher;
            analyzer = filenameAnalyzer;
            defaultField = DocumentIndexer.FIELD_FILENAME;
            indexLabel = "Filename Index";
        } else {
            if (contentSearcher == null) throw new IllegalStateException("L'indice dei contenuti non è aperto.");
            searcher = contentSearcher;
            analyzer = contentAnalyzer;
            defaultField = DocumentIndexer.FIELD_CONTENT;
            indexLabel = "Content Index";
        }

        QueryParser parser = new QueryParser(defaultField, analyzer);
        // Default operator: OR (standard Lucene)
        parser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = parser.parse(queryText);

        return executeQuery(searcher, query, queryText, indexLabel, topN);
    }

    /**
     * Esegue direttamente un oggetto Lucene Query (es. TermQuery, BooleanQuery, PhraseQuery).
     */
    public SearchResult executeQuery(TargetIndex target, Query query, String description, int topN) throws IOException {
        IndexSearcher searcher = (target == TargetIndex.FILENAME) ? filenameSearcher : contentSearcher;
        String indexLabel = (target == TargetIndex.FILENAME) ? "Filename Index" : "Content Index";
        if (searcher == null) throw new IllegalStateException("Searcher non disponibile per " + indexLabel);

        return executeQuery(searcher, query, description, indexLabel, topN);
    }

    private SearchResult executeQuery(IndexSearcher searcher, Query query, String queryText, String indexLabel, int topN) throws IOException {
        long start = System.nanoTime();
        TopDocs topDocs = searcher.search(query, topN);
        long elapsedNs = System.nanoTime() - start;
        long elapsedMs = elapsedNs / 1_000_000;

        List<SearchHit> hits = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            String filename = doc.get(DocumentIndexer.FIELD_FILENAME);
            String relPath = doc.get(DocumentIndexer.FIELD_PATH);
            String absPath = doc.get(DocumentIndexer.FIELD_ABSOLUTE_PATH);
            long size = 0L;
            var sizeField = doc.getField(DocumentIndexer.FIELD_SIZE);
            if (sizeField != null && sizeField.numericValue() != null) {
                size = sizeField.numericValue().longValue();
            }

            hits.add(new SearchHit(
                    scoreDoc.doc,
                    scoreDoc.score,
                    filename != null ? filename : "N/D",
                    relPath != null ? relPath : "N/D",
                    absPath != null ? absPath : "N/D",
                    size,
                    null
            ));
        }

        return new SearchResult(queryText, indexLabel, topDocs.totalHits.value, elapsedMs, hits);
    }

    /**
     * Costruisce programmaticamente una PhraseQuery per ricerca esatta su frase.
     */
    public static PhraseQuery createPhraseQuery(String field, String... terms) {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        for (String term : terms) {
            builder.add(new Term(field, term.toLowerCase()));
        }
        return builder.build();
    }

    /**
     * Costruisce programmaticamente una BooleanQuery.
     */
    public static BooleanQuery createBooleanQuery(BooleanClause... clauses) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause clause : clauses) {
            builder.add(clause);
        }
        return builder.build();
    }

    public IndexReader getFilenameReader() {
        return filenameReader;
    }

    public IndexReader getContentReader() {
        return contentReader;
    }

    @Override
    public synchronized void close() throws IOException {
        if (filenameReader != null) {
            filenameReader.close();
            filenameReader = null;
        }
        if (contentReader != null) {
            contentReader.close();
            contentReader = null;
        }
        if (filenameAnalyzer != null) {
            filenameAnalyzer.close();
            filenameAnalyzer = null;
        }
        if (contentAnalyzer != null) {
            contentAnalyzer.close();
            contentAnalyzer = null;
        }
    }
}
