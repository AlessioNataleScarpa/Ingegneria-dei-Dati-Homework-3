package it.uniroma3.hw3.benchmark;

import it.uniroma3.hw3.model.TargetIndex;

import java.net.URI;
import java.util.List;

/**
 * Enumerazione che modella i corpus di benchmark standard di letteratura IR.
 * Ciascun dominio include l'origine HTTP dei documenti e una suite di query autentiche derivate dalla letteratura.
 */
public enum BenchmarkCorpus {

    CISI(
            "CISI",
            "Information Science & Biblioteconomia",
            URI.create("https://raw.githubusercontent.com/GianRomani/CISI-project-MLOps/refs/heads/main/CISI.ALL"),
            URI.create("https://raw.githubusercontent.com/GianRomani/CISI-project-MLOps/refs/heads/main/CISI.QRY"),
            "cisi",
            List.of(
                    new BenchmarkQuery("titles", TargetIndex.FILENAME, "Term", "CISI.QRY Topic 1: problemi di retrieval da titoli descrittivi"),
                    new BenchmarkQuery("dewey_decimal", TargetIndex.FILENAME, "Delimiters", "Titolo Doc 1: 18 Editions of the Dewey Decimal Classifications"),
                    new BenchmarkQuery("information AND retrieval", TargetIndex.FILENAME, "Boolean AND", "CISI.QRY Topic 5: sistemi di information retrieval"),
                    new BenchmarkQuery("\"computer systems\"", TargetIndex.FILENAME, "Phrase", "CISI.QRY Topic 7: computer systems for publishing"),
                    new BenchmarkQuery("retrieving", TargetIndex.CONTENT, "Stemming", "CISI.QRY Topic 1: verifica stemming radice 'retriev'"),
                    new BenchmarkQuery("indexing", TargetIndex.CONTENT, "High-Frequency Term", "Corpus CISI: frequenza elevata, misura latenza posting list"),
                    new BenchmarkQuery("information AND retrieval", TargetIndex.CONTENT, "Boolean AND", "CISI.QRY Topic 2 e Topic 5: intersezione concetti cardine"),
                    new BenchmarkQuery("retrieval NOT automatic", TargetIndex.CONTENT, "Boolean NOT", "CISI.QRY Topic 1: filtraggio booleano negativo"),
                    new BenchmarkQuery("\"use of information retrieval systems\"", TargetIndex.CONTENT, "Phrase con Stop-words", "CISI.QRY Topic 5: concordanza posizionale scavalcando stop-word 'of'")
            )
    ),

    CRANFIELD(
            "Cranfield",
            "Ingegneria Aeronautica & Aerodinamica",
            URI.create("https://raw.githubusercontent.com/Kmahmoudi/Information-Retrieval-System/master/cran.all.1400"),
            null,
            "cran",
            List.of(
                    new BenchmarkQuery("aerodynamics", TargetIndex.FILENAME, "Term", "Titolo Doc 1: aerodynamics of a wing in a slipstream"),
                    new BenchmarkQuery("boundary_layer", TargetIndex.FILENAME, "Delimiters", "Abstract Doc 2: boundary-layer flow"),
                    new BenchmarkQuery("supersonic AND flow", TargetIndex.FILENAME, "Boolean AND", "Titoli Doc 12 e Doc 45: aerodinamica supersonica"),
                    new BenchmarkQuery("\"pressure distribution\"", TargetIndex.FILENAME, "Phrase", "Titolo Doc 18: pressure distribution on wings"),
                    new BenchmarkQuery("vibrations", TargetIndex.CONTENT, "Stemming", "Abstract Doc 24: verifica stemming radice 'vibrat'"),
                    new BenchmarkQuery("viscous", TargetIndex.CONTENT, "Term", "Corpus Cranfield: flusso viscoso"),
                    new BenchmarkQuery("mach AND number", TargetIndex.CONTENT, "Boolean AND", "Abstracts Cranfield: regimi di Mach"),
                    new BenchmarkQuery("flow NOT laminar", TargetIndex.CONTENT, "Boolean NOT", "Abstracts Cranfield: esclusione flusso laminare"),
                    new BenchmarkQuery("\"boundary layer\"", TargetIndex.CONTENT, "Phrase", "Abstract Doc 2: frase posizionale aerodinamica")
            )
    ),

    NEWSGROUPS(
            "20 Newsgroups (Comp/Sci)",
            "Informatica, Hardware & Cyber-Security",
            URI.create("https://archive.ics.uci.edu/static/public/113/twenty+newsgroups.zip"),
            null,
            "news",
            List.of(
                    new BenchmarkQuery("hardware", TargetIndex.FILENAME, "Term", "Newsgroup comp.sys.ibm.pc.hardware"),
                    new BenchmarkQuery("ibm_pc", TargetIndex.FILENAME, "Delimiters", "Subject autentico newsgroup con underscore"),
                    new BenchmarkQuery("graphics AND windows", TargetIndex.FILENAME, "Boolean AND", "Newsgroups comp.graphics e comp.os.ms-windows"),
                    new BenchmarkQuery("\"operating system\"", TargetIndex.FILENAME, "Phrase", "Discussioni architetturali sui sistemi operativi"),
                    new BenchmarkQuery("encryption", TargetIndex.CONTENT, "Stemming", "Newsgroup sci.crypt: verifica stemming radice 'encrypt'"),
                    new BenchmarkQuery("clipper", TargetIndex.CONTENT, "Term", "Newsgroup sci.crypt: chip crittografico Clipper"),
                    new BenchmarkQuery("public AND key", TargetIndex.CONTENT, "Boolean AND", "Newsgroup sci.crypt: crittografia asimmetrica"),
                    new BenchmarkQuery("security NOT rsa", TargetIndex.CONTENT, "Boolean NOT", "Newsgroup sci.crypt: sicurezza escludendo RSA"),
                    new BenchmarkQuery("\"public key\"", TargetIndex.CONTENT, "Phrase", "Newsgroup sci.crypt: ricerca posizionale frase esatta")
            )
    );

    private final String displayName;
    private final String domain;
    private final URI documentSourceUri;
    private final URI querySourceUri;
    private final String filePrefix;
    private final List<BenchmarkQuery> calibratedQueries;

    BenchmarkCorpus(String displayName, String domain, URI documentSourceUri, URI querySourceUri, String filePrefix, List<BenchmarkQuery> calibratedQueries) {
        this.displayName = displayName;
        this.domain = domain;
        this.documentSourceUri = documentSourceUri;
        this.querySourceUri = querySourceUri;
        this.filePrefix = filePrefix;
        this.calibratedQueries = List.copyOf(calibratedQueries);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDomain() {
        return domain;
    }

    public URI getDocumentSourceUri() {
        return documentSourceUri;
    }

    public URI getQuerySourceUri() {
        return querySourceUri;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public List<BenchmarkQuery> getCalibratedQueries() {
        return calibratedQueries;
    }
}
