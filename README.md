# Homework 3 - Ingegneria dei Dati: Motore di Ricerca con Apache Lucene

Progetto completo per l'Homework 3 del corso di **Ingegneria dei Dati**, basato su **Apache Lucene 9.12.0** e **Java 21 LTS**.

---

## 🚀 Caratteristiche Principali
- **Doppio Indice Fisico Indipendente**:
  - `indexes/filename_index`: Indice A per i nomi di file (tokenizzazione basata su regex delimitatori, lowercase, no stop-words né stemming).
  - `indexes/content_index`: Indice B per il testo (elisione italiana, ASCII folding accenti, stop-words italiano/inglese e stemming morfologico Snowball).
- **Searcher conforme alla traccia**:
  - Sintassi: `nome <query>` e `contenuto <query>`.
  - Supporto a **TermQuery**, **BooleanQuery** (`AND`, `OR`, `NOT`) e **PhraseQuery** esatta con virgolette `"..."`.
  - Stampa ordinata: hit count, tempo di esecuzione in ms, score probabilistico BM25, nome file e path relativo.
- **Suite di Benchmark Sperimentale**:
  - Generatore controllato di corpus sintetico (100–1.000+ file .txt).
  - Misurazione tempi di indicizzazione, throughput (docs/s e MB/s), spazio su disco e latenze medie su 12 diverse tipologie di query.
- **Relazione Tecnica Completa**:
  - Documento [RELAZIONE.md](RELAZIONE.md) pronto per la consegna del 13 Novembre 2026 corredato da dati sperimentali reali.

---

## 🛠️ Requisiti di Sistema
- **Java**: JDK 21 LTS o superiore.
- **Maven**: Apache Maven 3.8+ o 3.9+.

---

## 📦 Compilazione ed Esecuzione Test

Dalla cartella principale del progetto:

```bash
# 1. Compilazione pulita ed esecuzione dei test unitari (JUnit 5)
mvn clean test

# 2. Copia delle dipendenze runtime nella cartella target/lib
mvn dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime -q
```

---

## 🔍 Esecuzione dell'Applicazione (CLI)

### Modalità Interattiva
Avvia la console interattiva con menu a scelta:
```bash
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp
```
All'interno della console è possibile:
1. Indicizzare qualsiasi cartella locale contenente file `.txt`.
2. Eseguire ricerche digitando `nome <query>` oppure `contenuto <query>`.
3. Lanciare il benchmark automatizzato.
4. Generare dataset di test sintetici di qualsiasi dimensione.

### Modalità Parametrica Diretta da Terminale
Puoi eseguire ricerche immediate senza avviare il menu interattivo:

```bash
# Ricerca sul nome del file per singolo termine
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp search nome ingegneria

# Ricerca sul nome del file con PhraseQuery esatta
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp search nome "\"ingegneria dei dati\""

# Ricerca sul contenuto con operatori booleani (AND)
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp search contenuto database AND ottimizzazione

# Ricerca sul contenuto con operatore NOT
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp search contenuto algoritmo AND NOT deadlock

# Ricerca sul contenuto con PhraseQuery esatta
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp search contenuto "\"indice inverso\""
```

### Esecuzione Benchmark Sperimentale
```bash
java -cp "target/classes;target/lib/*" it.uniroma3.ingegneriadeidati.hw3.cli.SearchApp benchmark 1000
```

---

## 📊 Sintesi Risultati Sperimentali (1.000 Documenti)

| Metrica | Indice A (Filename) | Indice B (Content) |
| :--- | :--- | :--- |
| **Documenti indicizzati** | 1.000 | 1.000 |
| **Tempo di indicizzazione** | **976 ms** | **1.610 ms** |
| **Dimensione su disco** | **66,75 KB** | **365,49 KB** |
| **Throughput indicizzazione** | **1.024,6 docs/s** | **621,1 docs/s** |
| **Latenza media di ricerca** | **2,879 ms** | **2,165 ms** |

Per l'analisi teorica completa, le formule BM25 e la spiegazione dettagliata degli Analyzer, consulta [RELAZIONE.md](RELAZIONE.md).
