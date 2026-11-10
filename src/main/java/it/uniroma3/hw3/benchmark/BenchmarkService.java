package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.model.BenchmarkReport;

import java.nio.file.Path;

/**
 * Service interface per l'esecuzione di benchmark comparativi e raccolta metriche empiriche.
 */
public interface BenchmarkService {

    /**
     * Esegue il benchmark completo (preparazione, indicizzazione A e B, warm-up e suite di query)
     * su un singolo corpus di letteratura.
     *
     * @param corpus       il corpus target (es. CISI, CRANFIELD, NEWSGROUPS)
     * @param baseDataDir  directory in cui memorizzare/estrarre i file di testo del corpus
     * @param baseIndexDir directory in cui allocare gli indici Lucene temporanei
     * @return il report dettagliato del benchmark
     * @throws BenchmarkException in caso di errore durante la procedura
     */
    BenchmarkReport runBenchmark(BenchmarkCorpus corpus, Path baseDataDir, Path baseIndexDir) throws BenchmarkException;

    /**
     * Esegue il benchmark su tutti i corpora di letteratura configurati e produce
     * il report globale aggregato multi-dominio.
     *
     * @param baseDataDir  directory base per i dati
     * @param baseIndexDir directory base per gli indici
     * @return report globale comparativo
     * @throws BenchmarkException in caso di errore
     */
    MultiDomainBenchmarkReport runAllBenchmarks(Path baseDataDir, Path baseIndexDir) throws BenchmarkException;
}
