package it.uniroma3.ingegneriadeidati.hw3.cli;

import it.uniroma3.ingegneriadeidati.hw3.benchmark.BenchmarkRunner;
import it.uniroma3.ingegneriadeidati.hw3.benchmark.DatasetGenerator;
import it.uniroma3.ingegneriadeidati.hw3.indexer.DocumentIndexer;
import it.uniroma3.ingegneriadeidati.hw3.model.IndexStats;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchHit;
import it.uniroma3.ingegneriadeidati.hw3.model.SearchResult;
import it.uniroma3.ingegneriadeidati.hw3.searcher.DocumentSearcher;
import it.uniroma3.ingegneriadeidati.hw3.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Applicazione CLI Principale per l'Homework 3 - Ingegneria dei Dati.
 * Offre un'interfaccia interattiva a riga di comando per:
 * 1. Indicizzare directory di file di testo (personalizzate o generate).
 * 2. Eseguire ricerche flessibili con la sintassi richiesta:
 *    - "nome <termini | \"phrase query\">"
 *    - "contenuto <termini | \"phrase query\">"
 * 3. Eseguire benchmark completi e produrre dati per la relazione tecnica.
 */
public class SearchApp {

    private static final Path DEFAULT_CORPUS_DIR = Paths.get("data", "documents");
    private static final Path DEFAULT_INDEX_DIR = Paths.get("indexes");
    private static final Path DEFAULT_FILENAME_INDEX = DEFAULT_INDEX_DIR.resolve("filename_index");
    private static final Path DEFAULT_CONTENT_INDEX = DEFAULT_INDEX_DIR.resolve("content_index");

    public static void main(String[] args) {
        // Se avviato con flag non interattivo (es. benchmark diretto)
        if (args.length > 0) {
            handleArgs(args);
            return;
        }

        printBanner();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                System.out.print("Scegli un'opzione [1-6]: ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> interactiveIndexing(scanner);
                    case "2" -> interactiveSearch(scanner);
                    case "3" -> interactiveBenchmark(scanner);
                    case "4" -> generateDatasetInteractive(scanner);
                    case "5" -> printStatus();
                    case "6", "exit", "quit" -> {
                        System.out.println("Chiusura applicazione. Arrivederci!");
                        running = false;
                    }
                    default -> System.out.println("Opzione non valida. Riprova.");
                }
                System.out.println();
            }
        }
    }

    private static void handleArgs(String[] args) {
        String cmd = args[0].toLowerCase();
        try {
            switch (cmd) {
                case "index" -> {
                    Path corpus = args.length > 1 ? Paths.get(args[1]) : DEFAULT_CORPUS_DIR;
                    System.out.println("Avvio indicizzazione automatica da: " + corpus);
                    DocumentIndexer indexer = new DocumentIndexer();
                    List<IndexStats> stats = indexer.indexCorpus(corpus, DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX);
                    for (IndexStats s : stats) {
                        System.out.printf("Indice %s: %d documenti indicizzati in %d ms (Dimensione: %s)%n",
                                s.indexName(), s.documentCount(), s.durationMs(), FileUtils.formatBytes(s.diskSizeBytes()));
                    }
                }
                case "benchmark" -> {
                    int count = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
                    System.out.printf("Generazione dataset di %d documenti in %s...%n", count, DEFAULT_CORPUS_DIR);
                    DatasetGenerator.generateDataset(DEFAULT_CORPUS_DIR, count, 250, 42L);
                    BenchmarkRunner runner = new BenchmarkRunner();
                    BenchmarkRunner.BenchmarkReport report = runner.runBenchmark(
                            DEFAULT_CORPUS_DIR, DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX, 5
                    );
                    System.out.println("\n" + report.toMarkdown());
                }
                case "search" -> {
                    if (args.length < 3) {
                        System.out.println("Uso: search <nome|contenuto> <query>");
                        return;
                    }
                    String target = args[1].toLowerCase();
                    DocumentSearcher.TargetIndex ti = target.startsWith("nom") ?
                            DocumentSearcher.TargetIndex.FILENAME : DocumentSearcher.TargetIndex.CONTENT;
                    
                    // Unisce tutti i restanti argomenti per gestire query multi-parola o quotate da terminale
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) sb.append(" ");
                        sb.append(args[i]);
                    }
                    String queryText = sb.toString().trim();
                    if (queryText.startsWith("\"") && queryText.endsWith("\"") && queryText.length() >= 2) {
                        queryText = queryText.substring(1, queryText.length() - 1);
                        queryText = "\"" + queryText + "\"";
                    }

                    try (DocumentSearcher searcher = new DocumentSearcher(DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX)) {
                        SearchResult res = searcher.search(ti, queryText, 10);
                        printSearchResult(res);
                    }
                }
                default -> System.out.println("Comando non riconosciuto: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("Errore nell'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printBanner() {
        System.out.println("======================================================================");
        System.out.println("   HOMEWORK 3 - INGEGNERIA DEI DATI: APACHE LUCENE SEARCH ENGINE      ");
        System.out.println("   Indice Nomi File + Indice Contenuto con Stemming e Analisi IR      ");
        System.out.println("======================================================================");
    }

    private static void printMenu() {
        System.out.println("--- MENU PRINCIPALE ---");
        System.out.println("1. Indicizza Documenti (Filename Index + Content Index)");
        System.out.println("2. Ricerca Interattiva (sintassi: nome <query> | contenuto <query>)");
        System.out.println("3. Esegui Benchmark Sperimentale Completo");
        System.out.println("4. Genera Dataset Sintetico di Test");
        System.out.println("5. Stato degli Indici su Disco");
        System.out.println("6. Esci");
    }

    private static void interactiveIndexing(Scanner scanner) {
        System.out.print("Percorso cartella corpus [.txt] (invio per default '" + DEFAULT_CORPUS_DIR + "'): ");
        String input = scanner.nextLine().trim();
        Path corpus = input.isEmpty() ? DEFAULT_CORPUS_DIR : Paths.get(input);

        if (!Files.exists(corpus)) {
            System.out.println("La cartella specificata non esiste: " + corpus);
            System.out.print("Vuoi generare automaticamente 500 documenti di test qui? (s/n): ");
            String ans = scanner.nextLine().trim();
            if (ans.equalsIgnoreCase("s") || ans.equalsIgnoreCase("si") || ans.equalsIgnoreCase("y")) {
                try {
                    DatasetGenerator.generateDataset(corpus, 500, 200, 42L);
                    System.out.println("Dataset di prova generato con successo.");
                } catch (IOException e) {
                    System.err.println("Errore nella generazione: " + e.getMessage());
                    return;
                }
            } else {
                return;
            }
        }

        try {
            System.out.println("Avvio indicizzazione...");
            DocumentIndexer indexer = new DocumentIndexer();
            List<IndexStats> stats = indexer.indexCorpus(corpus, DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX);

            System.out.println("\n=== INDICIZZAZIONE COMPLETATA CON SUCCESSO ===");
            for (IndexStats s : stats) {
                System.out.printf("Indice: %-15s | Documenti: %-6d | Tempo: %-5d ms | Dimensione: %-10s | Throughput: %.1f docs/s%n",
                        s.indexName(), s.documentCount(), s.durationMs(),
                        FileUtils.formatBytes(s.diskSizeBytes()), s.getThroughputDocsPerSec());
            }
        } catch (Exception e) {
            System.err.println("Errore durante l'indicizzazione: " + e.getMessage());
        }
    }

    private static void interactiveSearch(Scanner scanner) {
        if (!Files.exists(DEFAULT_FILENAME_INDEX) || !Files.exists(DEFAULT_CONTENT_INDEX)) {
            System.out.println("Attenzione: Uno o entrambi gli indici non esistono su disco.");
            System.out.println("Esegui prima l'opzione 1 (Indicizza Documenti).");
            return;
        }

        try (DocumentSearcher searcher = new DocumentSearcher(DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX)) {
            System.out.println("\nModalità Ricerca Attiva.");
            System.out.println("Sintassi accettata:");
            System.out.println(" - nome <termini>            (es: nome relazione dati)");
            System.out.println(" - nome \"<frase esatta>\"     (es: nome \"ingegneria dei dati\")");
            System.out.println(" - contenuto <termini>       (es: contenuto database AND transazioni)");
            System.out.println(" - contenuto \"<frase>\"       (es: contenuto \"indice inverso\")");
            System.out.println(" - Digita 'back' per tornare al menu.");

            while (true) {
                System.out.print("\nQuery> ");
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("back") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }

                DocumentSearcher.TargetIndex target;
                String queryText;

                if (line.toLowerCase().startsWith("nome ")) {
                    target = DocumentSearcher.TargetIndex.FILENAME;
                    queryText = line.substring(5).trim();
                } else if (line.toLowerCase().startsWith("contenuto ")) {
                    target = DocumentSearcher.TargetIndex.CONTENT;
                    queryText = line.substring(10).trim();
                } else {
                    System.out.println("Prefisso non valido! Specifica 'nome <query>' oppure 'contenuto <query>'.");
                    continue;
                }

                if (queryText.isEmpty()) {
                    System.out.println("La query non può essere vuota.");
                    continue;
                }

                try {
                    SearchResult result = searcher.search(target, queryText, 10);
                    printSearchResult(result);
                } catch (Exception e) {
                    System.out.println("Errore nel parsing/esecuzione della query: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nell'apertura degli indici: " + e.getMessage());
        }
    }

    private static void printSearchResult(SearchResult res) {
        System.out.printf("%n[INDICE: %s] Query: \"%s\"%n", res.targetIndex(), res.queryText());
        System.out.printf("Documenti trovati (Hits): %,d | Tempo di risposta: %d ms%n",
                res.totalHits(), res.executionTimeMs());
        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.printf("%-6s | %-8s | %-35s | %s%n", "Rank", "BM25 Score", "Nome File", "Percorso Relativo");
        System.out.println("-------------------------------------------------------------------------------------------------");

        if (res.hits().isEmpty()) {
            System.out.println("   (Nessun documento corrispondente ai criteri di ricerca)");
        } else {
            int rank = 1;
            for (SearchHit hit : res.hits()) {
                System.out.printf("#%-5d | %-10.4f | %-35s | %s%n",
                        rank++, hit.score(), hit.filename(), hit.relativePath());
            }
        }
        System.out.println("-------------------------------------------------------------------------------------------------");
    }

    private static void interactiveBenchmark(Scanner scanner) {
        System.out.print("Numero di documenti per il benchmark sintetico [default: 1000]: ");
        String countStr = scanner.nextLine().trim();
        int count = countStr.isEmpty() ? 1000 : Integer.parseInt(countStr);

        try {
            System.out.printf("1. Generazione di %d file in %s...%n", count, DEFAULT_CORPUS_DIR);
            DatasetGenerator.generateDataset(DEFAULT_CORPUS_DIR, count, 250, 42L);

            System.out.println("2. Avvio suite di benchmark completa...");
            BenchmarkRunner runner = new BenchmarkRunner();
            BenchmarkRunner.BenchmarkReport report = runner.runBenchmark(
                    DEFAULT_CORPUS_DIR, DEFAULT_FILENAME_INDEX, DEFAULT_CONTENT_INDEX, 5
            );

            System.out.println("\n" + report.toMarkdown());

            Path reportFile = Paths.get("BENCHMARK_REPORT.md");
            Files.writeString(reportFile, report.toMarkdown());
            System.out.println("Report salvato anche su file: " + reportFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione del benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateDatasetInteractive(Scanner scanner) {
        System.out.print("Percorso destinazione [default: " + DEFAULT_CORPUS_DIR + "]: ");
        String pathStr = scanner.nextLine().trim();
        Path dir = pathStr.isEmpty() ? DEFAULT_CORPUS_DIR : Paths.get(pathStr);

        System.out.print("Numero di file da creare [default: 500]: ");
        String countStr = scanner.nextLine().trim();
        int count = countStr.isEmpty() ? 500 : Integer.parseInt(countStr);

        try {
            DatasetGenerator.generateDataset(dir, count, 200, System.currentTimeMillis());
            System.out.printf("Generati con successo %d documenti in: %s%n", count, dir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Errore durante la generazione: " + e.getMessage());
        }
    }

    private static void printStatus() {
        System.out.println("\n=== STATO INDICI SU DISCO ===");
        printIndexInfo("Indice A (Filename)", DEFAULT_FILENAME_INDEX);
        printIndexInfo("Indice B (Content)", DEFAULT_CONTENT_INDEX);
    }

    private static void printIndexInfo(String label, Path path) {
        if (!Files.exists(path)) {
            System.out.printf("- %s: NON PRESENTE (%s)%n", label, path);
            return;
        }
        long size = FileUtils.calculateDirectorySize(path);
        try (var stream = Files.list(path)) {
            long files = stream.count();
            System.out.printf("- %s: PRESENTE | Segmenti: %d file | Dimensione su disco: %s (%s)%n",
                    label, files, FileUtils.formatBytes(size), path);
        } catch (IOException e) {
            System.out.printf("- %s: Errore lettura: %s%n", label, e.getMessage());
        }
    }
}
