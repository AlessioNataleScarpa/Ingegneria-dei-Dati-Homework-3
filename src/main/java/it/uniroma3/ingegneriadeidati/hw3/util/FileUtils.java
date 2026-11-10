package it.uniroma3.ingegneriadeidati.hw3.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

/**
 * Utilità di sistema per manipolazione di file e directory.
 */
public final class FileUtils {

    private FileUtils() {}

    /**
     * Calcola la dimensione totale di una cartella in byte ricorsivamente.
     */
    public static long calculateDirectorySize(Path path) {
        if (path == null || !Files.exists(path)) {
            return 0L;
        }
        try (var stream = Files.walk(path)) {
            return stream.filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Cancella ricorsivamente una cartella o un file se esiste.
     */
    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Formatta una dimensione in byte in stringa human-readable (B, KB, MB, GB).
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
