package it.uniroma3.ingegneriadeidati.hw3.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerTest {

    private List<String> tokenize(Analyzer analyzer, String text) throws IOException {
        List<String> result = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream("test_field", text)) {
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                result.add(attr.toString());
            }
            stream.end();
        }
        return result;
    }

    @Test
    @DisplayName("FilenameAnalyzer: spezza su delimitatori comuni (trattini, underscore, punti) e fa lowercase")
    void testFilenameAnalyzerSplitting() throws IOException {
        try (FilenameAnalyzer analyzer = new FilenameAnalyzer()) {
            List<String> tokens = tokenize(analyzer, "Relazione_HW3-Ingegneria.Dei.Dati_v2.txt");
            
            // Ci aspettiamo: relazione, hw3, ingegneria, dei, dati, v2, txt
            assertTrue(tokens.contains("relazione"));
            assertTrue(tokens.contains("hw3"));
            assertTrue(tokens.contains("ingegneria"));
            assertTrue(tokens.contains("dei"));
            assertTrue(tokens.contains("dati"));
            assertTrue(tokens.contains("v2"));
            assertTrue(tokens.contains("txt"));
            assertFalse(tokens.contains("Relazione")); // deve essere lowercase
        }
    }

    @Test
    @DisplayName("ContentAnalyzer: applica elisione, lowercase, stop-words, rimozione accenti e stemming italiano")
    void testContentAnalyzerStemmingAndStopwords() throws IOException {
        try (ContentAnalyzer analyzer = new ContentAnalyzer()) {
            List<String> tokens = tokenize(analyzer, "Gli studenti studiano l'algoritmo di memorizzazione");

            assertFalse(tokens.contains("gli"), "Stop-word 'gli' deve essere rimossa");
            assertFalse(tokens.contains("di"), "Stop-word 'di' deve essere rimossa");

            // Verifica elisione: l'algoritmo -> token "algoritm" (non "l'algoritmo")
            assertTrue(tokens.contains("algoritm"), "L'elisione l' deve essere rimossa e la parola stemmata ad algoritm");
            assertTrue(tokens.contains("student"));
            assertTrue(tokens.contains("stud"));
            assertTrue(tokens.contains("memorizz"));
        }
    }
}
