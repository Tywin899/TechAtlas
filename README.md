# TechAtlas

> **Discover Engineering. Faster.**

TechAtlas is a backend-first engineering knowledge search engine that aggregates content from multiple trusted technical sources into a unified search experience. Instead of searching across several websites individually, TechAtlas provides a single interface for discovering engineering knowledge with fast, relevant, and explainable search results.

The project is built from scratch to demonstrate the core principles behind modern search engines, including information retrieval, indexing, ranking, and scalable backend architecture.

---

## ✨ Features

* Unified search across multiple engineering knowledge sources
* BM25-based document ranking
* Custom inverted index implementation
* Incremental indexing for efficient updates
* RESTful API built with Spring Boot
* Autocomplete using a Trie data structure
* Search analytics and query tracking
* Extensible architecture for adding new content sources

---

## 📚 Planned Content Sources

| Source         | Status  |
| -------------- | ------- |
| Wikipedia      | Planned |
| GitHub         | Planned |
| Reddit         | Planned |
| Stack Overflow | Planned |

Each source is integrated through a common ingestion pipeline, making the system easy to extend without changing the search engine core.

---

## 🏗️ System Architecture

```text
External APIs
       │
       ▼
Fetcher Service
       │
       ▼
Parser
       │
       ▼
PostgreSQL (Documents)
       │
       ▼
Index Builder
       │
       ▼
Inverted Index + BM25 Statistics
       │
       ▼
Search Service
       │
       ▼
REST API
```

The project follows a **Modular Monolith** architecture, where each subsystem has a clearly defined responsibility while remaining simple to develop and deploy.

---

## 🔍 Search Pipeline

```text
User Query
    │
    ▼
Normalize
    │
    ▼
Tokenize
    │
    ▼
Remove Stop Words
    │
    ▼
Stem Terms
    │
    ▼
Lookup Posting Lists
    │
    ▼
Generate Candidates
    │
    ▼
BM25 Ranking
    │
    ▼
Return Ranked Results
```

---

## 🧰 Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Data JPA
* Maven

### Database

* PostgreSQL
* Flyway

### Search

* Inverted Index
* BM25 Ranking
* Trie Autocomplete

### Tooling

* Docker
* Swagger / OpenAPI
* GitHub Actions (planned)

---

## 📁 Project Structure

```text
src/
├── controller/
├── service/
├── fetcher/
├── parser/
├── index/
├── ranking/
├── autocomplete/
├── repository/
├── entity/
├── dto/
├── config/
├── util/
└── exception/
```

---

## 🚀 Development Roadmap

* [ ] Project setup
* [ ] Database schema
* [ ] Wikipedia integration
* [ ] Document ingestion
* [ ] Inverted index
* [ ] BM25 ranking
* [ ] Search API
* [ ] Incremental indexing
* [ ] GitHub integration
* [ ] Reddit integration
* [ ] Stack Overflow integration
* [ ] Redis caching
* [ ] Autocomplete
* [ ] Analytics dashboard

---

## 🎯 Learning Objectives

This project is designed to explore and implement:

* Information Retrieval fundamentals
* Search engine architecture
* Index construction and maintenance
* Ranking algorithms
* REST API design
* Backend scalability
* Clean software architecture
* Modular system design

---

## 📖 Documentation

Project documentation is available in the `docs/` directory.

* **01-Architecture.md** — System architecture and design
* **02-Database.md** — Database schema and storage design
* **03-API.md** — REST API specification
* **04-Roadmap.md** — Development milestones

---

## 🤝 Contributing

Contributions, discussions, and suggestions are welcome. If you'd like to improve the project, feel free to open an issue or submit a pull request.

---

## 📄 License

This project is released under the MIT License.
