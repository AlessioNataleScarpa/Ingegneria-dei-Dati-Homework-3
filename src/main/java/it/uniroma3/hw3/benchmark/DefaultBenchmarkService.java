package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.analysis.AnalyzerStrategy;
import it.uniroma3.hw3.analysis.DefaultAnalyzerProvider;
import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.indexing.IndexManager;
import it.uniroma3.hw3.indexing.LuceneDocumentBuilder;
import it.uniroma3.hw3.indexing.LuceneIndexManager;
import it.uniroma3.hw3.model.BenchmarkReport;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;
import it.uniroma3.hw3.searching.LuceneSearchService;
import it.uniroma3.hw3.searching.SearchService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Implementazione predefinita del servizio di benchmark per i corpora della letteratura IR.
 */
public class DefaultBenchmarkService implements BenchmarkService {

    private static final int WARMUP_RUNS = 3;
    private final CorpusDatasetManager datasetManager;
    private final AnalyzerStrategy analyzerStrategy;
    private final IndexManager indexManager;

    public DefaultBenchmarkService() {
        this(new CorpusDatasetManager(),
             new DefaultAnalyzerProvider(),
             new LuceneIndexManager(new DefaultAnalyzerProvider()));
    }

    public DefaultBenchmarkService(CorpusDatasetManager datasetManager,
                                   AnalyzerStrategy analyzerStrategy,
                                   IndexManager indexManager) {
        this.datasetManager = Objects.requireNonNull(datasetManager, "datasetManager non può essere nullo");
        this.analyzerStrategy = Objects.requireNonNull(analyzerStrategy, "analyzerStrategy non può essere nullo");
        this.indexManager = Objects.requireNonNull(indexManager, "indexManager non può essere nullo");
    }

    @Override
    public BenchmarkReport runBenchmark(BenchmarkCorpus corpus, Path baseDataDir, Path baseIndexDir) throws BenchmarkException {
        Objects.requireNonNull(corpus, "corpus non può essere nullo");
        Objects.requireNonNull(baseDataDir, "baseDataDir non può essere nullo");
        Objects.requireNonNull(baseIndexDir, "baseIndexDir non può essere nullo");

        try {
            // 1. Preparazione del corpus (.txt)
            Path corpusDir = datasetManager.prepareCorpus(corpus, baseDataDir);

            // 2. Percorsi per Indice A e Indice B
            Path filenameIndexPath = baseIndexDir.resolve(corpus.getFilePrefix() + "_filename_idx");
            Path contentIndexPath = baseIndexDir.resolve(corpus.getFilePrefix() + "_content_idx");

            // 3. Indicizzazione Indice A (Filename)
            IndexMetrics filenameMetrics = indexManager.index(corpusDir, filenameIndexPath, TargetIndex.FILENAME);

            // 4. Indicizzazione Indice B (Content)
            IndexMetrics contentMetrics = indexManager.index(corpusDir, contentIndexPath, TargetIndex.CONTENT);

            // 5. Esecuzione e misurazione della suite di query autentiche
            List<BenchmarkReport.QueryBenchmarkResult> queryResults = new ArrayList<>();
            double totalFilenameLatency = 0.0;
            int countFilenameQueries = 0;
            double totalContentLatency = 0.0;
            int countContentQueries = 0;

            try (SearchService searchService = new LuceneSearchService(filenameIndexPath, contentIndexPath, analyzerStrategy)) {
                // Warm-up per riscaldare JIT e page cache di Lucene
                warmup(searchService, corpus.getCalibratedQueries());

                for (BenchmarkQuery bq : corpus.getCalibratedQueries()) {
                    long startNano = System.nanoTime();
                    SearchResult result = searchService.search(bq.targetIndex(), bq.queryText(), 10);
                    long elapsedNano = System.nanoTime() - startNano;
                    double latencyMs = elapsedNano / 1_000_000.0;

                    queryResults.add(new BenchmarkReport.QueryBenchmarkResult(
                            bq.queryText(),
                            bq.targetIndex(),
                            bq.queryType(),
                            result.totalHits(),
                            latencyMs
                    ));

                    if (bq.targetIndex() == TargetIndex.FILENAME) {
                        totalFilenameLatency += latencyMs;
                        countFilenameQueries++;
                    } else {
                        totalContentLatency += latencyMs;
                        countContentQueries++;
                    }
                }
            }

            double avgFilenameLatency = countFilenameQueries > 0 ? (totalFilenameLatency / countFilenameQueries) : 0.0;
            double avgContentLatency = countContentQueries > 0 ? (totalContentLatency / countContentQueries) : 0.0;

            return new BenchmarkReport(
                    filenameMetrics.documentCount(),
                    filenameMetrics.corpusSizeBytes(),
                    filenameMetrics,
                    contentMetrics,
                    queryResults,
                    avgFilenameLatency,
                    avgContentLatency
            );

        } catch (IndexingException | SearchException e) {
            throw new BenchmarkException("Errore durante l'esecuzione del benchmark su " + corpus.getDisplayName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public MultiDomainBenchmarkReport runAllBenchmarks(Path baseDataDir, Path baseIndexDir) throws BenchmarkException {
        Map<BenchmarkCorpus, BenchmarkReport> reports = new LinkedHashMap<>();
        for (BenchmarkCorpus corpus : BenchmarkCorpus.values()) {
            reports.put(corpus, runBenchmark(corpus, baseDataDir, baseIndexDir));
        }
        return new MultiDomainBenchmarkReport(reports);
    }

    private void warmup(SearchService searchService, List<BenchmarkQuery> queries) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            for (BenchmarkQuery q : queries) {
                try {
                    searchService.search(q.targetIndex(), q.queryText(), 5);
                } catch (SearchException ignored) {
                    // Ignora le eccezioni di parsing durante la fase preliminare di warm-up
                }
            }
        }
    }
}
