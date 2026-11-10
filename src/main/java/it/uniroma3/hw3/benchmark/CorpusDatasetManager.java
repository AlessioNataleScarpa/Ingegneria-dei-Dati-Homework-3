package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.exception.BenchmarkException;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Pure Fabrication responsabile del reperimento, caching e scomposizione
 * dei dataset storici di letteratura IR in file .txt individuali pronti per l'indicizzazione.
 */
public class CorpusDatasetManager {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;

    public CorpusDatasetManager() {
        this(HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build());
    }

    public CorpusDatasetManager(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient non può essere nullo");
    }

    /**
     * Prepara il corpus specificato: se i file .txt sono già presenti nella directory di destinazione,
     * sfrutta la cache locale senza riscaricare; altrimenti scarica il dataset master ed estrae i file .txt.
     *
     * @param corpus      il corpus da preparare (CISI, CRANFIELD, NEWSGROUPS)
     * @param baseDataDir directory base per i dati (es. data/corpora)
     * @return il percorso della cartella contenente i singoli file .txt pronti
     * @throws BenchmarkException in caso di errore di download o I/O
     */
    public Path prepareCorpus(BenchmarkCorpus corpus, Path baseDataDir) throws BenchmarkException {
        Objects.requireNonNull(corpus, "corpus non può essere nullo");
        Objects.requireNonNull(baseDataDir, "baseDataDir non può essere nullo");

        Path corpusOutputDir = baseDataDir.resolve(corpus.getFilePrefix());
        Path cacheDir = baseDataDir.resolve(".cache");

        try {
            Files.createDirectories(corpusOutputDir);
            Files.createDirectories(cacheDir);

            // Se la directory contiene già più di 100 file .txt, consideriamo il corpus già preparato
            long existingTxtFiles = countTxtFiles(corpusOutputDir);
            if (existingTxtFiles > 100) {
                return corpusOutputDir;
            }

            // Altrimenti procediamo con download ed estrazione
            Path rawMasterFile = cacheDir.resolve(corpus.name().toLowerCase() + "_master.raw");
            downloadIfMissing(corpus.getDocumentSourceUri(), rawMasterFile);

            switch (corpus) {
                case CISI -> extractCisiOrCranfield(rawMasterFile, corpusOutputDir, "cisi");
                case CRANFIELD -> extractCisiOrCranfield(rawMasterFile, corpusOutputDir, "cran");
                case NEWSGROUPS -> extractNewsgroupsSubset(rawMasterFile, corpusOutputDir, 1500);
            }

            return corpusOutputDir;

        } catch (IOException e) {
            throw new BenchmarkException("Impossibile preparare il corpus " + corpus.getDisplayName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Scarica un file remoto se non esiste già localmente.
     */
    public void downloadIfMissing(URI uri, Path targetFile) throws BenchmarkException {
        if (Files.exists(targetFile) && targetFile.toFile().length() > 0) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetFile));
            if (response.statusCode() >= 400) {
                Files.deleteIfExists(targetFile);
                throw new BenchmarkException(String.format("Download fallito per %s (HTTP %d)", uri, response.statusCode()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BenchmarkException("Download interrotto per " + uri, e);
        } catch (IOException e) {
            throw new BenchmarkException("Errore I/O durante il download di " + uri + ": " + e.getMessage(), e);
        }
    }

    /**
     * Esegue il parsing riga per riga di file con formato a tag dot-notation (.I, .T, .W) tipici di CISI e Cranfield.
     * Genera un file .txt autonomo per ogni documento.
     */
    public int extractCisiOrCranfield(Path masterFile, Path outputDir, String prefix) throws IOException {
        int docCount = 0;
        String currentId = null;
        StringBuilder currentTitle = new StringBuilder();
        StringBuilder currentContent = new StringBuilder();
        String currentSection = "";

        try (BufferedReader reader = Files.newBufferedReader(masterFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I ")) {
                    // Salva il documento precedente se presente
                    if (currentId != null && !currentContent.isEmpty()) {
                        writeDocumentFile(outputDir, prefix, currentId, currentTitle.toString().trim(), currentContent.toString().trim());
                        docCount++;
                    }
                    currentId = line.substring(3).trim();
                    currentTitle.setLength(0);
                    currentContent.setLength(0);
                    currentSection = "I";
                } else if (line.startsWith(".T")) {
                    currentSection = "T";
                } else if (line.startsWith(".A") || line.startsWith(".B") || line.startsWith(".X")) {
                    currentSection = line.substring(1, 2);
                } else if (line.startsWith(".W")) {
                    currentSection = "W";
                } else {
                    if ("T".equals(currentSection)) {
                        if (!currentTitle.isEmpty()) {
                            currentTitle.append(" ");
                        }
                        currentTitle.append(line.trim());
                    } else if ("W".equals(currentSection)) {
                        currentContent.append(line).append(System.lineSeparator());
                    }
                }
            }

            // Ultimo documento
            if (currentId != null && !currentContent.isEmpty()) {
                writeDocumentFile(outputDir, prefix, currentId, currentTitle.toString().trim(), currentContent.toString().trim());
                docCount++;
            }
        }

        return docCount;
    }

    /**
     * Estrae un sottoinsieme di file testuali da un archivio ZIP (es. 20 Newsgroups).
     */
    public int extractNewsgroupsSubset(Path zipFile, Path outputDir, int maxFiles) throws IOException {
        int extracted = 0;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null && extracted < maxFiles) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                // Consideriamo i gruppi tecnici rilevanti
                if (name.contains("comp.") || name.contains("sci.crypt") || name.contains("sci.space")) {
                    String sanitizedName = name.replace('/', '_').replace('\\', '_');
                    if (!sanitizedName.endsWith(".txt")) {
                        sanitizedName += ".txt";
                    }
                    Path outFile = outputDir.resolve(sanitizedName);
                    Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);
                    extracted++;
                }
            }
        }
        return extracted;
    }

    private void writeDocumentFile(Path outputDir, String prefix, String id, String title, String content) throws IOException {
        String safeTitle = sanitizeFileName(title);
        if (safeTitle.length() > 60) {
            safeTitle = safeTitle.substring(0, 60);
        }
        String fileName = String.format("%s_%04d%s.txt",
                prefix,
                Integer.parseInt(id),
                safeTitle.isEmpty() ? "" : "_" + safeTitle);

        Path filePath = outputDir.resolve(fileName);
        Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Sanifica una stringa per renderla un nome file valido su qualsiasi OS (incluso Windows).
     */
    public String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private long countTxtFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".txt")).count();
        }
    }
}
