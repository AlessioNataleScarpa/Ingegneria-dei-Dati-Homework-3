package it.uniroma3.hw3.analysis;

import it.uniroma3.hw3.model.TargetIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Suite per il Sottosistema di Analisi Lessicale")
class AnalyzerStrategyTest {

    private List<String> extractTokens(Analyzer analyzer, String fieldName, String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(fieldName, text)) {
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(attr.toString());
            }
            stream.end();
        }
        return tokens;
    }

    @Test
    @DisplayName("FilenameAnalyzer: tokenizzazione su delimitatori, lowercase e conservazione termini")
    void testFilenameAnalyzerTokenization() throws IOException {
        try (FilenameAnalyzer analyzer = new FilenameAnalyzer()) {
            List<String> tokens = extractTokens(analyzer, "filename", "relazione_ingegneria-dei-dati_v2.0.txt");

            assertTrue(tokens.contains("relazione"));
            assertTrue(tokens.contains("ingegneria"));
            assertTrue(tokens.contains("dei"));
            assertTrue(tokens.contains("dati"));
            assertTrue(tokens.contains("v2"));
            assertTrue(tokens.contains("0"));
            assertTrue(tokens.contains("txt"));
            assertFalse(tokens.contains("relazione_ingegneria"));
        }
    }

    @Test
    @DisplayName("ContentAnalyzer: pipeline completa con elisione, lowercase, rimozione accenti, stopwords e stemming")
    void testContentAnalyzerPipeline() throws IOException {
        try (ContentAnalyzer analyzer = new ContentAnalyzer()) {
            String text = "L'algoritmo distribuisce le informazioni perché gli studenti studiano";
            List<String> tokens = extractTokens(analyzer, "content", text);

            assertFalse(tokens.contains("le"), "Stop-word 'le' deve essere rimossa");
            assertFalse(tokens.contains("gli"), "Stop-word 'gli' deve essere rimossa");

            // Verifica elisione e stemming
            assertTrue(tokens.contains("algoritm"), "L'elisione 'l' deve essere separata e 'algoritmo' stemmata in 'algoritm'");
            // Stemming di "informazioni" produce "inform"
            assertTrue(tokens.contains("inform"), "Stemming di 'informazioni' deve produrre 'inform'");
            // Normalizzazione accenti: 'perché' -> 'perche' (stopword)
            assertFalse(tokens.contains("perché"));
            assertTrue(tokens.contains("student"));
            assertTrue(tokens.contains("stud"));
        }
    }

    @Test
    @DisplayName("DefaultAnalyzerProvider: mapping corretto per TargetIndex")
    void testDefaultAnalyzerProviderMapping() {
        try (AnalyzerStrategy strategy = new DefaultAnalyzerProvider()) {
            Analyzer fnAnalyzer = strategy.getAnalyzer(TargetIndex.FILENAME);
            Analyzer ctAnalyzer = strategy.getAnalyzer(TargetIndex.CONTENT);

            assertNotNull(fnAnalyzer);
            assertNotNull(ctAnalyzer);
            assertInstanceOf(FilenameAnalyzer.class, fnAnalyzer);
            assertInstanceOf(ContentAnalyzer.class, ctAnalyzer);
        }
    }
}
