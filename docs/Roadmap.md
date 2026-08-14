# Development Roadmap

## Overview

This roadmap outlines the planned implementation phases for TechAtlas. Each milestone delivers a functional increment that can be tested independently before moving on to the next phase.

---

# Phase 1 – Project Setup

**Objective:** Establish the project foundation.

### Tasks

* Create Spring Boot project
* Configure Maven
* Configure PostgreSQL
* Add Flyway migrations
* Configure Docker Compose
* Integrate Swagger/OpenAPI
* Create base package structure
* Configure logging

**Deliverable**

A runnable Spring Boot application connected to PostgreSQL.

---

# Phase 2 – Persistence Layer

**Objective:** Store and manage documents.

### Tasks

* Create `Document` entity
* Create repository layer
* Add Flyway migration scripts
* Implement CRUD operations
* Store metadata using JSONB

**Deliverable**

Documents can be persisted and retrieved from the database.

---

# Phase 3 – Document Ingestion

**Objective:** Fetch documents from external sources.

### Tasks

* Implement Wikipedia API integration
* Parse API responses
* Convert data into `Document` entities
* Store parsed documents
* Compute document content hash

**Deliverable**

Wikipedia documents are fetched and stored successfully.

---

# Phase 4 – Search Indexing Engine

**Objective:** Build the indexing engine pipeline and in-memory structures.

### Tasks

* [x] Tokenizer splitting, Unicode, and punctuation removal
* [x] TextNormalizer case, spacing, and numbers preservation
* [x] Configurable Stop-word removal filter
* [x] Porter Stemmer integration adapter
* [x] In-memory Inverted Index with posting lists
* [x] Ingestion pipeline service integration
* [x] Index management REST endpoints

**Deliverable**

Documents can be tokenized, normalized, stemmed, and indexed into an in-memory inverted index.

---

# Phase 5 – REST Search API & Ranking

**Objective:** Implement query processing, BM25 scoring, and expose search functionality via REST.

### Tasks

* [x] Query Processor mirroring document processing rules
* [x] BM25 scoring calculation logic
* [x] Score accumulation across multi-word queries
* [x] REST search endpoint (`GET /api/v1/search`)
* [x] Pagination logic and metadata responses
* [x] Excerpt matching snippet generator
* [x] Query parameters and boundary validations

**Deliverable**

Clients can search documents, retrieve ranked results with BM25 scores, get matching text snippets, and navigate pages via the REST search API.

---

# Phase 6 – Incremental Indexing

**Objective:** Avoid rebuilding the entire index.

### Tasks

* [x] Compare content hashes
* [x] Detect document changes
* [x] Update only affected index entries
* [x] Support scheduled indexing (mechanisms prepared, scheduling deferred to Phase 11)

**Deliverable**

* [x] Efficient index updates without full rebuilds.

---

# Phase 7 – Intelligent Wikipedia Discovery

**Objective:** Controlled category-based Wikipedia crawling and traversal.

### Tasks

* [x] Category member discovery API client
* [x] BFS category traversal up to maxDepth
* [x] Bound crawler by maxArticles limits
* [x] Visited category cycles protection
* [x] Visited tracking persistence in PostgreSQL
* [x] Bulk import via existing document pipeline

**Deliverable**

* [x] Controlled category-based discovery and synchronization.

---

# Phase 8 – GitHub Ingestion

**Objective:** Fetch, parse, and ingest GitHub repository READMEs and documents.

### Tasks

* [x] Official GitHub API repository search integration
* [x] GitHub API pagination and bounding
* [x] README markdown contents extraction and Base64 decoding
* [x] Persistent repository sync tracking table (`github_sync_repositories`)
* [x] Category/Query propagation to imported Documents
* [x] Reusing Phase 6 incremental indexing for repository updates

**Deliverable**

* [x] Bounded, controlled repository-level discovery and synchronization.

---

# Phase 9 – Stack Overflow Ingestion

**Objective:** Fetch, parse, and ingest Stack Overflow technical questions/answers.

### Tasks

* [x] Official Stack Exchange API questions search and answers retrieval
* [x] Lightweight HTML-to-text body parsing and entity decoding
* [x] Accepted and high-scoring answers sorting and consolidation
* [x] Persistent question synchronization log (`stackoverflow_sync_questions`)
* [x] Deduplication and incremental updates on question body changes

**Deliverable**

* [x] Bounded, controlled Stack Overflow discovery and ingestion.

---

# Phase 10 – Source Synchronization Engine

**Objective:** Implement a generalized synchronization framework tracking revisions/hashes across Wikipedia, GitHub, and Stack Overflow.

### Tasks

* [x] Database migration mapping `source_sync` records with uniqueness bounds
* [x] Abstract adapter interfaces decoupling generic orchestrators from source clients
* [x] Change detection logic leveraging external revisions and hash fallback comparisons
* [x] Document lifecycle integrations triggering incremental re-indexing on modifications
* [x] REST endpoints for manual synchronizations and tracking statistics

**Deliverable**

* [x] Multi-source capable generalized synchronization engine.

---

# Phase 11 – Automated Scheduling

**Objective:** Background task scheduling and periodic source synchronization.

### Tasks

* [x] Configurable background scheduler with custom initial/fixed delay bindings
* [x] Concurrency guard preventing multiple overlapping synchronizations for the same source
* [x] Failure isolation insuring one source failure does not affect other schedules
* [x] Expose scheduler metadata via status API `/api/v1/sync/scheduler/status`

**Deliverable**

* [x] Autonomous background scheduling orchestration engine.

---

# Phase 12 – Performance Improvements

**Objective:** Redis caching, query optimization, and performance scaling.

### Tasks

* [x] Integrated Redis container using Docker Compose configuration
* [x] Formulated type-safe properties configuration with configurable TTL settings
* [x] Created decoupled `CacheService` abstraction with transparent Redis failure fallback
* [x] Configured Jackson JSON serialization for stable record data serialization
* [x] Caching of document details with `document:{id}` eviction on updates
* [x] Caching of query matches via SHA-256 canonical keys, invalidated on index changes
* [x] Expose hits, misses, and evictions stats on `/api/v1/cache/status`

**Deliverable**

* [x] Fault-tolerant caching layer providing sub-millisecond search latencies.

---

# Phase 13 – Autocomplete & Search Suggestions

**Objective:** Trie-based autocomplete suggestions.

### Tasks

* [x] Formulated type-safe properties configuration with configurable popular and recent query limits
* [x] Implemented thread-safe `PrefixTrie` to host dictionary search terms
* [x] Embedded prefix trie synchronization hooks into `InvertedIndex` creation, eviction, and re-indexing cycles
* [x] Created `QueryTracker` recording search queries to Redis with thread-safe in-memory fallback
* [x] Designed `AutocompleteService` performing prefix term matching and popular query suggestions
* [x] Created `AutocompleteController` exposing suggestions on `GET /api/v1/search/suggestions` and stats on `GET /api/v1/autocomplete/status`

**Deliverable**

* [x] Fast, thread-safe, and fault-tolerant autocomplete suggestions API.

---

# Phase 14 – Analytics & Monitoring

**Objective:** Database persistent search analytics, JVM/Redis operational metric counters, and unified dashboard reporting endpoints with fault isolation boundaries.

### Tasks

* [x] Schema migration for database search metrics logs (`search_analytics`)
* [x] Perform high-performance JPQL projection mappings for top queries, zero results, and latency percentiles
* [x] Instrument indexing pipeline with AtomicLong operational counters tracking latencies
* [x] Instrument synchronization engine with in-memory duration and status mappings
* [x] Expose unified dashboard overview on `/api/v1/analytics/overview` and details on `/api/v1/analytics/*`

**Deliverable**

* [x] Robust, fault-isolated analytics and operational monitoring subsystem.

---

Potential improvements include:

* Semantic search
* Vector embeddings
* Learning-to-rank
* Personalized search
* Synonym expansion
* Distributed indexing
* Elasticsearch integration
* Web frontend

---

# Success Criteria

The MVP is considered complete when it supports:

* ✅ Document ingestion
* ✅ Persistent storage
* ✅ Inverted index creation
* ✅ BM25-based ranking
* ✅ REST search API
* ✅ Incremental indexing
* ✅ Autocomplete
* ✅ Analytics & Monitoring
* ✅ OpenAPI documentation

---

# Long-Term Vision

TechAtlas aims to become a practical demonstration of modern Information Retrieval concepts and backend engineering best practices. The project will evolve incrementally, prioritizing clean architecture, extensibility, and maintainability while remaining accessible as a learning resource.
