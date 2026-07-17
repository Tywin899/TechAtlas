# API Specification

## Overview

TechAtlas exposes a RESTful API that allows clients to search indexed documents, manage indexing operations, and retrieve system information.

All endpoints return JSON responses and follow REST conventions.

**Base URL**

/api/v1


---

# Search API

## Search Documents

http
GET /search


Searches indexed documents using the BM25 ranking algorithm.

### Query Parameters

| Parameter | Type    | Required | Description                    |
| --------- | ------- | -------- | ------------------------------ |
| q         | String  | ✅        | Search query                   |
| page      | Integer | ❌        | Page number (default: 0)       |
| size      | Integer | ❌        | Results per page (default: 10) |
| source    | String  | ❌        | Filter by content source       |

### Example Request

http
GET /api/v1/search?q=spring%20boot&page=0&size=10


### Example Response

json
{
  "query": "spring boot",
  "page": 0,
  "size": 10,
  "totalResults": 127,
  "results": [
    {
      "id": 1,
      "title": "Spring Boot",
      "source": "Wikipedia",
      "url": "https://...",
      "score": 15.82,
      "snippet": "Spring Boot is an open-source Java framework..."
    }
  ]
}


---

# Autocomplete API

## Get Search Suggestions

http
GET /autocomplete


Returns autocomplete suggestions using the Trie.

### Query Parameters

| Parameter | Type   | Required |
| --------- | ------ | -------- |
| q         | String | ✅        |

### Example Request

http
GET /api/v1/autocomplete?q=spri


### Example Response

```json
{
  "suggestions": [
    "spring",
    "spring boot",
    "spring security"
  ]
}
```

---

# Document API

## Get Document by ID

```http
GET /documents/{id}
```

Returns complete information for a stored document.

### Example Response

```json
{
  "id": 42,
  "title": "Spring Boot",
  "source": "Wikipedia",
  "url": "https://...",
  "content": "...",
  "metadata": {}
}
```

---

# Index Management API

## Trigger Incremental Index Update

```http
POST /index/update
```

Fetches new or updated documents and updates the search index.

### Response

```json
{
  "status": "SUCCESS",
  "message": "Incremental indexing completed."
}
```

---

## Rebuild Search Index

```http
POST /index/rebuild
```

Rebuilds the entire search index from stored documents.

### Response

```json
{
  "status": "SUCCESS",
  "indexedDocuments": 15234
}
```

---

## Index Status

```http
GET /index/status
```

Returns the current indexing status.

### Example Response

```json
{
  "status": "READY",
  "lastIndexed": "2026-07-17T09:30:00Z",
  "documentsIndexed": 15234
}
```

---

# Health API

## Application Health

```http
GET /health
```

Used by monitoring systems to verify application health.

### Example Response

```json
{
  "status": "UP"
}
```

---

# Response Codes

| Code | Meaning               |
| ---- | --------------------- |
| 200  | Request successful    |
| 201  | Resource created      |
| 400  | Invalid request       |
| 404  | Resource not found    |
| 500  | Internal server error |
| 503  | Service unavailable   |

---

# Standard Error Response

All errors follow a consistent format.

```json
{
  "timestamp": "2026-07-17T09:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Query parameter 'q' is required.",
  "path": "/api/v1/search"
}
```

---

# API Design Principles

* RESTful endpoint naming
* JSON request and response bodies
* Consistent HTTP status codes
* Pagination for search results
* Stateless request handling
* OpenAPI (Swagger) documentation for all endpoints

---

# Future Endpoints

The following endpoints are planned for future releases:

| Endpoint                      | Purpose                               |
| ----------------------------- | ------------------------------------- |
| `GET /sources`                | List configured content providers     |
| `GET /analytics`              | Retrieve search analytics             |
| `GET /analytics/trending`     | Most searched topics                  |
| `GET /search/explain`         | Explain BM25 scoring for a result     |
| `POST /sources/{source}/sync` | Synchronize a specific content source |

---

# Summary

| Category     | Endpoints                                                        |
| ------------ | ---------------------------------------------------------------- |
| Search       | `GET /search`                                                    |
| Autocomplete | `GET /autocomplete`                                              |
| Documents    | `GET /documents/{id}`                                            |
| Indexing     | `POST /index/update`, `POST /index/rebuild`, `GET /index/status` |
| Health       | `GET /health`                                                    |

The API is designed to remain stable as new content providers and search features are added.
