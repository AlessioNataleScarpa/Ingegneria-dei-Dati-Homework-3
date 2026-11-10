package it.uniroma3.ingegneriadeidati.hw3.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generatore controllato di corpus di file .txt per benchmarking e verifiche sperimentali.
 * Simula documenti strutturati, relazioni, report aziendali, file di log e note tecniche
 * con frequenze terminologiche e lunghezze variabili.
 */
public class DatasetGenerator {

    private static final String[] TOPICS = {
            "ingegneria_dei_dati", "basi_di_dati", "architettura_software", "information_retrieval",
            "sistemi_distribuiti", "cloud_computing", "analisi_algoritmi", "machine_learning",
            "progettazione_web", "sicurezza_informatica"
    };

    private static final String[] VOCABULARY = {
            "database", "relazionale", "indice", "inverso", "query", "ottimizzazione", "prestazioni",
            "algoritmo", "complessita", "distribuito", "cluster", "memoria", "archiviazione", "throughput",
            "latenza", "server", "client", "rete", "protocollo", "sicurezza", "crittografia", "autenticazione",
            "transazione", "acid", "consistenza", "scalabilita", "partizionamento", "pipeline", "etl",
            "bigdata", "elaborazione", "flusso", "parallelo", "concorrenza", "lock", "deadlock", "processore",
            "grafi", "alberi", "ricerca", "ordinamento", "statistica", "probabilita", "modello", "framework",
            "sviluppo", "test", "benchmark", "misurazione", "risultato", "conclusione", "relazione", "progetto"
    };

    private static final String[] FILE_EXT_PREFIXES = {
            "relazione", "appunti", "capitolo", "documento", "specifica", "analisi", "benchmark_test", "riassunto"
    };

    /**
     * Genera un dataset sintetico nella directory di output.
     *
     * @param targetDir  Cartella in cui creare i file
     * @param fileCount  Numero di file da generare (es. 100, 500, 1000)
     * @param wordsPerFile Lunghezza media del testo in parole
     * @param seed       Seed pseudocasuale per riproducibilità
     */
    public static void generateDataset(Path targetDir, int fileCount, int wordsPerFile, long seed) throws IOException {
        Files.createDirectories(targetDir);
        Random rand = new Random(seed);

        for (int i = 1; i <= fileCount; i++) {
            String topic = TOPICS[rand.nextInt(TOPICS.length)];
            String prefix = FILE_EXT_PREFIXES[rand.nextInt(FILE_EXT_PREFIXES.length)];
            int version = rand.nextInt(10) + 1;
            // Esempio nome: relazione_ingegneria_dei_dati_v3_0042.txt oppure appunti-basi_di_dati-0042.txt
            String separator = (rand.nextBoolean()) ? "_" : "-";
            String filename = String.format("%s%s%s%sv%d%s%04d.txt",
                    prefix, separator, topic, separator, version, separator, i);

            Path filePath = targetDir.resolve(filename);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write("TITOLO: " + filename.replace(".txt", "").replace('_', ' ').replace('-', ' '));
                writer.newLine();
                writer.write("ARGOMENTO: " + topic);
                writer.newLine();
                writer.write("ID DOCUMENTO: " + i);
                writer.newLine();
                writer.newLine();

                int actualWords = wordsPerFile + (rand.nextInt(wordsPerFile / 2) - wordsPerFile / 4);
                int lineCount = 0;
                for (int w = 0; w < actualWords; w++) {
                    String word = VOCABULARY[rand.nextInt(VOCABULARY.length)];
                    writer.write(word);
                    writer.write(" ");
                    lineCount++;
                    if (lineCount >= 12) {
                        writer.newLine();
                        lineCount = 0;
                    }
                }
                writer.newLine();
            }
        }
    }
}
