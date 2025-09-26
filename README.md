# Caching Service (Java 8, Spring Boot, PostgreSQL)

Implemented a simple LRU cache with:
- Configurable max size (default 3, set in `application.yml`)
- Eviction policy: least recently used entry saved to Postgres
- REST APIs with Swagger UI
- Global exception handling, logging, and unit tests 

## Quick start

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run tests
mvn clean verify

# 3. Run the app
mvn spring-boot:run
Swagger: http://localhost:8080/swagger-ui.html
```
## Endpoints (via Swagger)
- `POST /cache/add` — add entity (evicts LRU to Postgres on overflow)
- `GET /cache/get/{id}` — read-through (cache first; DB on miss; re-caches)
- `DELETE /cache/remove/{id}` — delete-through (cache + DB)
- `DELETE /cache/removeAll` — clear cache + DB
- `DELETE /cache/clear` — clear cache only
