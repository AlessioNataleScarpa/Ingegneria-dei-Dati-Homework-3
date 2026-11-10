package it.uniroma3.hw3.cli;

import it.uniroma3.hw3.analysis.DefaultAnalyzerProvider;
import it.uniroma3.hw3.benchmark.BenchmarkCorpus;
import it.uniroma3.hw3.benchmark.BenchmarkService;
import it.uniroma3.hw3.benchmark.DefaultBenchmarkService;
import it.uniroma3.hw3.benchmark.MultiDomainBenchmarkReport;
import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.indexing.IndexManager;
import it.uniroma3.hw3.indexing.LuceneIndexManager;
import it.uniroma3.hw3.model.BenchmarkReport;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import it.uniroma3.hw3.searching.LuceneSearchService;
import it.uniroma3.hw3.searching.SearchService;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementazione concreta di {@link SearchEngineFacade}.
 * Incapsula l'orchestrazione di {@link IndexManager}, {@link SearchService} e {@link BenchmarkService}.
 */
public class DefaultSearchEngineFacade implements SearchEngineFacade {

    private static final Path DEFAULT_FILENAME_INDEX_DIR = Path.of("indexes", "filename_index");
    private static final Path DEFAULT_CONTENT_INDEX_DIR = Path.of("indexes", "content_index");
    private static final Path DEFAULT_BENCHMARK_DATA_DIR = Path.of("data", "corpora");
    private static final Path DEFAULT_BENCHMARK_INDEX_DIR = Path.of("indexes", "benchmarks");

    private final Path filenameIndexDir;
    private final Path contentIndexDir;
    private final Path benchmarkDataDir;
    private final Path benchmarkIndexDir;

    private final IndexManager indexManager;
    private final SearchService searchService;
    private final BenchmarkService benchmarkService;

    /**
     * Costruttore standard con directory predefinite.
     */
    public DefaultSearchEngineFacade() {
        this(DEFAULT_FILENAME_INDEX_DIR, DEFAULT_CONTENT_INDEX_DIR, DEFAULT_BENCHMARK_DATA_DIR, DEFAULT_BENCHMARK_INDEX_DIR);
    }

    /**
     * Costruttore configurabile con directory specifiche.
     *
     * @param filenameIndexDir percorso indice dei nomi
     * @param contentIndexDir percorso indice dei contenuti
     * @param benchmarkDataDir directory per download/estrazione corpus benchmark
     * @param benchmarkIndexDir directory base per indici di benchmark
     */
    public DefaultSearchEngineFacade(Path filenameIndexDir, Path contentIndexDir, Path benchmarkDataDir, Path benchmarkIndexDir) {
        this.filenameIndexDir = Objects.requireNonNull(filenameIndexDir, "filenameIndexDir non può essere null");
        this.contentIndexDir = Objects.requireNonNull(contentIndexDir, "contentIndexDir non può essere null");
        this.benchmarkDataDir = Objects.requireNonNull(benchmarkDataDir, "benchmarkDataDir non può essere null");
        this.benchmarkIndexDir = Objects.requireNonNull(benchmarkIndexDir, "benchmarkIndexDir non può essere null");

        DefaultAnalyzerProvider analyzerProvider = new DefaultAnalyzerProvider();
        this.indexManager = new LuceneIndexManager(analyzerProvider);
        this.searchService = new LuceneSearchService(filenameIndexDir, contentIndexDir, analyzerProvider);
        this.benchmarkService = new DefaultBenchmarkService();
    }

    /**
     * Costruttore per Dependency Injection (utilizzabile per testing con mock o directory custom).
     *
     * @param filenameIndexDir percorso indice dei nomi
     * @param contentIndexDir percorso indice dei contenuti
     * @param benchmarkDataDir directory dati benchmark
     * @param benchmarkIndexDir directory indici benchmark
     * @param indexManager gestore indicizzazione
     * @param searchService servizio di ricerca
     * @param benchmarkService servizio benchmark
     */
    public DefaultSearchEngineFacade(Path filenameIndexDir,
                                     Path contentIndexDir,
                                     Path benchmarkDataDir,
                                     Path benchmarkIndexDir,
                                     IndexManager indexManager,
                                     SearchService searchService,
                                     BenchmarkService benchmarkService) {
        this.filenameIndexDir = Objects.requireNonNull(filenameIndexDir);
        this.contentIndexDir = Objects.requireNonNull(contentIndexDir);
        this.benchmarkDataDir = Objects.requireNonNull(benchmarkDataDir);
        this.benchmarkIndexDir = Objects.requireNonNull(benchmarkIndexDir);
        this.indexManager = Objects.requireNonNull(indexManager, "IndexManager non può essere null");
        this.searchService = Objects.requireNonNull(searchService, "SearchService non può essere null");
        this.benchmarkService = Objects.requireNonNull(benchmarkService, "BenchmarkService non può essere null");
    }

    @Override
    public Map<TargetIndex, IndexMetrics> indexCorpus(Path corpusDir) throws IndexingException {
        Objects.requireNonNull(corpusDir, "corpusDir non può essere null");
        List<IndexMetrics> metricsList = indexManager.indexAll(corpusDir, filenameIndexDir, contentIndexDir);
        Map<TargetIndex, IndexMetrics> metricsMap = new EnumMap<>(TargetIndex.class);
        for (IndexMetrics m : metricsList) {
            metricsMap.put(m.targetIndex(), m);
        }
        try {
            // Aggiorniamo i Searcher NRT
            searchService.refresh();
        } catch (SearchException e) {
            throw new IndexingException("Errore durante il refresh del searcher dopo l'indicizzazione", e);
        }
        return metricsMap;
    }

    @Override
    public SearchResult search(TargetIndex target, String queryString, int maxHits) throws SearchException {
        Objects.requireNonNull(target, "TargetIndex non può essere null");
        Objects.requireNonNull(queryString, "queryString non può essere null");
        return searchService.search(target, queryString, maxHits);
    }

    @Override
    public BenchmarkReport runBenchmark(BenchmarkCorpus corpus) throws BenchmarkException {
        Objects.requireNonNull(corpus, "BenchmarkCorpus non può essere null");
        return benchmarkService.runBenchmark(corpus, benchmarkDataDir, benchmarkIndexDir);
    }

    @Override
    public MultiDomainBenchmarkReport runAllBenchmarks() throws BenchmarkException {
        return benchmarkService.runAllBenchmarks(benchmarkDataDir, benchmarkIndexDir);
    }

    @Override
    public void close() {
        try {
            searchService.close();
        } catch (Exception ignored) {
            // Rilascio sicuro delle risorse
        }
    }
}
