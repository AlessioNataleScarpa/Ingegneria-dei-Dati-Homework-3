package it.uniroma3.hw3.cli;

import it.uniroma3.hw3.benchmark.BenchmarkCorpus;
import it.uniroma3.hw3.benchmark.MultiDomainBenchmarkReport;
import it.uniroma3.hw3.exception.BenchmarkException;
import it.uniroma3.hw3.exception.IndexingException;
import it.uniroma3.hw3.exception.SearchEngineException;
import it.uniroma3.hw3.exception.SearchException;
import it.uniroma3.hw3.model.BenchmarkReport;
import it.uniroma3.hw3.model.IndexMetrics;
import it.uniroma3.hw3.model.SearchHit;
import it.uniroma3.hw3.model.SearchResult;
import it.uniroma3.hw3.model.TargetIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Applicazione Console (CLI) per il motore di ricerca Apache Lucene.
 * Fornisce sia una modalità interattiva a menu (REPL) sia una modalità a comando diretto (one-shot).
 */
public class SearchApp {

    private static final String BANNER = """
            ================================================================================
               ___        __               __                     ___               
              / _ \\__ ___/ /____ _   ___  / /__ _____  ___ ___   / _ \\___ ___  ___ _
             / // / // _  / -_) / / / _ \\/  '_// _ \\/ -_) -_) / // / -_) _ \\/ _ `/
            /____/\\_,_/\\_,_/\\__/ / / .__/_/\\_\\/_//_/\\__/\\__/ /____/\\__/_//_/\\_,_/ 
                                  /_/  Apache Lucene 9.12.0 (Homework 3)        
            ================================================================================
            """;

    public static void main(String[] args) {
        try (SearchEngineFacade facade = new DefaultSearchEngineFacade()) {
            if (args == null || args.length == 0) {
                runInteractive(facade);
            } else {
                runDirectCommand(facade, args);
            }
        } catch (Exception e) {
            System.err.println("\n[ERRORE FATALE] " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Esegue il comando fornito da riga di comando (one-shot mode).
     */
    public static void runDirectCommand(SearchEngineFacade facade, String[] args) {
        String command = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (command) {
                case "index" -> handleIndexCommand(facade, args);
                case "search" -> handleSearchCommand(facade, args);
                case "benchmark" -> handleBenchmarkCommand(facade, args);
                case "help", "--help", "-h" -> printHelp();
                default -> {
                    System.err.println("Comando sconosciuto: '" + command + "'. Usa 'help' per la lista dei comandi.");
                    System.exit(1);
                }
            }
        } catch (SearchEngineException e) {
            System.err.println("\n[ERRORE] " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n[ERRORE IMPREVISTO] " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * Esegue il loop REPL interattivo.
     */
    public static void runInteractive(SearchEngineFacade facade) {
        System.out.println(BANNER);
        System.out.println("Benvenuto nella console interattiva. Digita 'help' per visualizzare i comandi disponibili.\n");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("lucene-cli> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = parseCommandTokens(line);
            if (tokens.length == 0) {
                continue;
            }

            String cmd = tokens[0].toLowerCase(Locale.ROOT);
            try {
                switch (cmd) {
                    case "exit", "quit", "q" -> {
                        System.out.println("Arrivederci!");
                        running = false;
                    }
                    case "help", "h", "?" -> printHelp();
                    case "index" -> handleIndexCommand(facade, tokens);
                    case "search" -> handleSearchCommand(facade, tokens);
                    case "benchmark" -> handleBenchmarkCommand(facade, tokens);
                    default -> System.out.println("Comando non riconosciuto: '" + cmd + "'. Digita 'help' per visualizzare la guida.");
                }
            } catch (SearchEngineException e) {
                System.err.println("\n[ERRORE] " + e.getMessage());
            } catch (Exception e) {
                System.err.println("\n[ERRORE IMPREVISTO] " + e.getMessage());
            }
            System.out.println();
        }
    }

    // =========================================================================
    // Gestione dei Singoli Comandi
    // =========================================================================

    private static void handleIndexCommand(SearchEngineFacade facade, String[] tokens) throws IndexingException {
        if (tokens.length < 2) {
            System.err.println("Uso: index <percorso_directory_corpus>");
            return;
        }
        Path corpusDir = Path.of(tokens[1]);
        if (!Files.exists(corpusDir) || !Files.isDirectory(corpusDir)) {
            System.err.println("Errore: Il percorso '" + corpusDir + "' non esiste o non è una cartella.");
            return;
        }

        System.out.println("\nAvvio indicizzazione del corpus in: " + corpusDir.toAbsolutePath() + " ...");
        Map<TargetIndex, IndexMetrics> metricsMap = facade.indexCorpus(corpusDir);

        System.out.println("\n================================================================================");
        System.out.println("ESITO INDICIZZAZIONE DOPPIA:");
        System.out.println("================================================================================");
        metricsMap.forEach((target, metrics) -> {
            System.out.printf(Locale.US, "Indice %-10s -> File: %d | Tempo: %d ms (%.2f s) | Spazio disco: %s (%.2f MB)%n",
                    target.name(),
                    metrics.documentCount(),
                    metrics.durationMs(),
                    metrics.durationMs() / 1000.0,
                    metrics.getFormattedDiskSize(),
                    metrics.getDiskSizeMB());
        });
        System.out.println("================================================================================");
    }

    private static void handleSearchCommand(SearchEngineFacade facade, String[] tokens) throws SearchException {
        if (tokens.length < 3) {
            System.err.println("Uso: search <nome|contenuto> <query_string> [-n <maxHits>]");
            return;
        }

        String targetStr = tokens[1].toLowerCase(Locale.ROOT);
        TargetIndex target;
        if (targetStr.equals("nome") || targetStr.equals("filename")) {
            target = TargetIndex.FILENAME;
        } else if (targetStr.equals("contenuto") || targetStr.equals("content")) {
            target = TargetIndex.CONTENT;
        } else {
            System.err.println("Indice non valido: '" + tokens[1] + "'. Usa 'nome' oppure 'contenuto'.");
            return;
        }

        // Ricostruzione query e parametri opzionali
        int maxHits = 10;
        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 2; i < tokens.length; i++) {
            if (tokens[i].equals("-n") && i + 1 < tokens.length) {
                try {
                    maxHits = Integer.parseInt(tokens[i + 1]);
                    i++; // salta il valore
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            if (!queryBuilder.isEmpty()) {
                queryBuilder.append(" ");
            }
            queryBuilder.append(tokens[i]);
        }

        String queryString = queryBuilder.toString().trim();
        if (queryString.isEmpty()) {
            System.err.println("La query non può essere vuota.");
            return;
        }

        SearchResult result = facade.search(target, queryString, maxHits);
        renderSearchResult(result, target);
    }

    private static void handleBenchmarkCommand(SearchEngineFacade facade, String[] tokens) throws BenchmarkException {
        String targetCorpus = (tokens.length > 1) ? tokens[1].toLowerCase(Locale.ROOT) : "all";

        System.out.println("\nAvvio sessione di benchmark scientifico (target: " + targetCorpus + ") ...");

        switch (targetCorpus) {
            case "cisi" -> {
                BenchmarkReport report = facade.runBenchmark(BenchmarkCorpus.CISI);
                System.out.println(report.toMarkdown());
            }
            case "cranfield", "cran" -> {
                BenchmarkReport report = facade.runBenchmark(BenchmarkCorpus.CRANFIELD);
                System.out.println(report.toMarkdown());
            }
            case "newsgroups", "news" -> {
                BenchmarkReport report = facade.runBenchmark(BenchmarkCorpus.NEWSGROUPS);
                System.out.println(report.toMarkdown());
            }
            case "all" -> {
                MultiDomainBenchmarkReport multiReport = facade.runAllBenchmarks();
                System.out.println(multiReport.toMarkdown());
            }
            default -> System.err.println("Corpus non riconosciuto: '" + targetCorpus + "'. Opzioni: cisi, cranfield, newsgroups, all.");
        }
    }

    // =========================================================================
    // Rendering e Presentazione
    // =========================================================================

    private static void renderSearchResult(SearchResult result, TargetIndex target) {
        System.out.println("\n================================================================================");
        System.out.printf(Locale.US, "RISULTATI DELLA RICERCA [Indice: %s] - Query: \"%s\"%n",
                target.name(), result.queryText());
        System.out.printf(Locale.US, "Trovati %d documenti in %d ms (mostrati i primi %d):%n",
                result.totalHits(), result.executionTimeMs(), result.hits().size());
        System.out.println("================================================================================");

        if (result.hits().isEmpty()) {
            System.out.println("Nessun documento trovato corrispondente ai criteri di ricerca.");
            System.out.println("================================================================================");
            return;
        }

        int rank = 1;
        for (SearchHit hit : result.hits()) {
            System.out.printf(Locale.US, "#%d [Score: %.4f]%n", rank++, hit.score());
            System.out.println("   File: " + hit.filename());
            System.out.println("   Path: " + hit.relativePath());
            if (hit.snippet() != null && !hit.snippet().isBlank()) {
                System.out.println("   Snippet: " + hit.snippet().trim());
            }
            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    private static void printHelp() {
        System.out.println("""
                COMANDI DISPONIBILI:
                --------------------------------------------------------------------------------
                  index <percorso_dir>
                      Indicizza tutti i file .txt della cartella indicata, popolando
                      in modo separato l'Indice A (Nome file) e l'Indice B (Contenuto).
                
                  search <nome|contenuto> <query> [-n <maxHits>]
                      Esegue una ricerca con ranking BM25 sull'indice selezionato.
                      Supporta:
                        - Termini singoli:     search contenuto retrieval
                        - Operatori booleani:  search contenuto information AND systems NOT database
                        - Frasi esatte:        search contenuto "information retrieval"
                        - Opzione hit massimi: search contenuto query -n 20
                
                  benchmark [cisi|cranfield|newsgroups|all]
                      Esegue i benchmark accademici su collezioni IR standard:
                        - cisi:       1.460 doc di Information Science
                        - cranfield:  1.400 doc di Aerodinamica / Aeronautica
                        - newsgroups: ~1.500 post di Computer Science & Cyber-Security
                        - all:        Esegue tutti e 3 i domini e stampa la tabella comparativa
                
                  help, h
                      Mostra questa guida rapida.
                
                  exit, quit, q
                      Termina la sessione interattiva.
                --------------------------------------------------------------------------------
                """);
    }

    /**
     * Helper per il parsing dei token da riga di comando che preserva le frasi racchiuse tra virgolette.
     */
    private static String[] parseCommandTokens(String line) {
        java.util.List<String> list = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
        while (m.find()) {
            String token = m.group(1).trim();
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                token = token.substring(1, token.length() - 1);
            }
            list.add(token);
        }
        return list.toArray(new String[0]);
    }
}
