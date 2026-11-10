# Homework 3 - Ingegneria dei Dati: Motore di Ricerca con Apache Lucene

Progetto completo per l'Homework 3 del corso di **Ingegneria dei Dati**, basato su **Apache Lucene 9.12.0** e **Java 21 LTS**.

---

## 🚀 Caratteristiche Principali
- **Doppio Indice Fisico Indipendente**:
  - `indexes/filename_index`: **Indice A** per i nomi e i percorsi dei file (tokenizzazione basata su regex delimitatori `[_\-\.\s\(\)\[\]\+]+`, lowercase, no stop-words né stemming per preservare estensioni, codici e componenti semantiche).
  - `indexes/content_index`: **Indice B** per il testo dei documenti (tokenizzazione conforme Unicode UAX#29, elisione italiana `ElisionFilter`, rimozione accenti `ASCIIFoldingFilter`, stop-words bilingue IT+EN e stemming morfologico Snowball `ItalianStemmer`).
- **Motore di Ricerca Conforme alle Specifiche**:
  - Sintassi: `nome <query>` e `contenuto <query>`.
  - Supporto a **TermQuery** (termine singolo), **BooleanQuery** (`AND`, `OR`, `NOT`) e **PhraseQuery** esatta posizionale con virgolette `"..."`.
  - Stampa ordinata con metadati completi: hit totali, tempo di esecuzione in millisecondi, score probabilistico BM25, nome del file e percorso relativo/assoluto.
- **Suite di Benchmark Multi-Dominio su Collezioni Reali di Letteratura IR**:
  - Integrazione automatica e download on-the-fly con caching di 3 collezioni consolidate:
    1. **CISI**: 1.460 abstract in *Information Science*.
    2. **Cranfield**: 1.398 abstract in *Ingegneria Aeronautica*.
    3. **20 Newsgroups (Comp/Sci)**: 900 post Usenet reali in *Informatica, Hardware e Crittografia*.
  - Suite di 27 query autentiche derivate dai topic IR originali.
  - Misurazione tempi di indicizzazione, throughput (docs/s e MB/s), footprint su disco e latenze di risposta.
- **Architettura OOA/D Rigorosa**:
  - GRASP Controller / GoF Facade (`SearchEngineFacade`).
  - GoF Strategy per gli Analyzer (`AnalyzerStrategy`, `AnalyzerProvider`).
  - Separazione completa tra presentation layer CLI, logica applicativa e librerie native Lucene.
- **Relazione Tecnica Ufficiale**:
  - Disponibile nel file [RELAZIONE.md](RELAZIONE.md), corredata da diagrammi architetturali, spiegazioni linguistiche degli analyzer, dati sperimentali completi e dimostrazione matematica del ranking Okapi BM25.

---

## 🛠️ Requisiti di Sistema
- **Java**: JDK 21 LTS o superiore.
- **Maven**: Apache Maven 3.8+ o 3.9+.

---

## 📦 Compilazione ed Esecuzione Test

Dalla cartella principale del progetto:

```bash
# Compilazione ed esecuzione di tutti i test unitari e di integrazione (JUnit 5)
mvn clean test
```

---

## 🔍 Esecuzione dell'Applicazione (CLI)

Il progetto è configurato con `exec-maven-plugin` per avviare direttamente la classe principale `it.uniroma3.hw3.cli.SearchApp`.

### 1. Modalità Interattiva (REPL)
Avvia la console interattiva con prompt `lucene-cli>`:

```bash
mvn exec:java
```

All'interno della console è possibile digitare i comandi:
- `index <cartella>`: indicizza tutti i file `.txt` della cartella selezionata costruendo entrambi gli indici.
- `search nome <query>`: esegue una query sull'indice del nome del file.
- `search contenuto <query>`: esegue una query sull'indice del contenuto del file.
- `benchmark [all|cisi|cran|news]`: lancia la sessione di benchmark scientifico.
- `help`: mostra la guida rapida dei comandi.
- `exit`: termina la sessione.

### 2. Modalità One-Shot da Riga di Comando
È possibile eseguire comandi immediati passando gli argomenti via Maven:

```bash
# 1. Indicizzazione di una cartella locale
mvn exec:java -D"exec.args=index data/documents"

# 2. Ricerca sul nome del file per termine singolo
mvn exec:java -D"exec.args=search nome ingegneria"

# 3. Ricerca sul nome con PhraseQuery esatta
mvn exec:java -D"exec.args=search nome '\"dewey decimal\"'"

# 4. Ricerca sul contenuto con operatori booleani (AND)
mvn exec:java -D"exec.args=search contenuto 'database AND ottimizzazione'"

# 5. Ricerca sul contenuto con operatore NOT
mvn exec:java -D"exec.args=search contenuto 'retrieval NOT automatic'"

# 6. Ricerca sul contenuto con PhraseQuery posizionale
mvn exec:java -D"exec.args=search contenuto '\"boundary layer\"'"

# 7. Esecuzione benchmark multi-dominio completo
mvn exec:java -D"exec.args=benchmark all"
```

---

## 📊 Sintesi dei Risultati Sperimentali

La tabella seguente riassume i dati sperimentali reali raccolti su **3.758 documenti scientifici**:

| Metrica | CISI (1.460 doc) | Cranfield (1.398 doc) | 20 Newsgroups (900 doc) |
| :--- | :---: | :---: | :---: |
| **Tempo Indice A (Filename)** | 1.557 ms | 535 ms | 352 ms |
| **Tempo Indice B (Content)** | 2.285 ms | 1.862 ms | 1.748 ms |
| **Dimensione Indice A** | 196,02 KB | 203,83 KB | 115,97 KB |
| **Dimensione Indice B** | 491,95 KB | 535,13 KB | 920,05 KB |
| **Throughput Indice B** | 638,9 docs/s | 750,8 docs/s | 514,9 docs/s |
| **Latenza Media Indice A** | 2,035 ms | 1,807 ms | 1,349 ms |
| **Latenza Media Indice B** | 8,613 ms | 11,477 ms | 12,257 ms |

Per la discussione teorica completa, le scelte degli Analyzer e l'analisi matematica di BM25, fare riferimento a [RELAZIONE.md](RELAZIONE.md).

---

## 📁 Struttura del Progetto

```
HW3/
├── pom.xml                                   # Configurazione Maven e dipendenze Lucene 9.12.0
├── RELAZIONE.md                              # Relazione tecnica ufficiale per il docente
├── README.md                                 # Documentazione operativa e guida rapida
├── indexes/                                  # Directory degli indici Lucene fisici
│   ├── filename_index/                       # Indice A: Nomi dei file
│   └── content_index/                        # Indice B: Contenuto dei file
├── data/
│   ├── corpora/                              # Cache locale e dataset estratti (cisi, cran, news)
│   └── documents/                            # Cartella per file di test utente
└── src/
    ├── main/java/it/uniroma3/hw3/
    │   ├── model/                            # Domain Model & Value Objects immutabili (SearchHit, IndexMetrics)
    │   ├── exception/                        # Gerarchia delle eccezioni tipizzate
    │   ├── analysis/                         # FilenameAnalyzer, ContentAnalyzer, AnalyzerStrategy
    │   ├── indexing/                         # LuceneDocumentBuilder, LuceneIndexManager
    │   ├── searching/                        # LuceneSearchService (BM25, NRT, QueryParser)
    │   ├── benchmark/                        # BenchmarkService, CorpusDatasetManager (CISI, Cranfield, Newsgroups)
    │   └── cli/                              # SearchEngineFacade, SearchApp (CLI interattiva e one-shot)
    └── test/java/it/uniroma3/hw3/            # Suite di test unitari JUnit 5
```
