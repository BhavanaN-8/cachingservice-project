package com.project.cache.service;

import com.project.cache.domain.EntityModel;
import com.project.cache.exception.ResourceNotFoundException;
import com.project.cache.persistence.PersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU cache (LinkedHashMap in access order).
 * When size exceeds max, evicts least-recently-used entry to Postgres.
 */
@Service
public class CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final PersistencePort database;
    private final int maxSize;
    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedHashMap<String, EntityModel> lru;

    public CacheService(PersistencePort database, @Value("${cache.maxSize:3}") int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        this.database = database;
        this.maxSize = maxSize;
        this.lru = new LinkedHashMap<String, EntityModel>(16, 0.75f, true);
    }

    /** add/update in cache; on overflow, evict eldest to DB */
    public void add(EntityModel entity) {
        if (entity == null || entity.getId() == null || entity.getId().trim().isEmpty())
            throw new IllegalArgumentException("id must not be null/blank");

        lock.lock();
        try {
            lru.put(entity.getId(), entity);
            log.info("Added to cache: {}", entity);
            if (lru.size() > maxSize) {
                Map.Entry<String, EntityModel> eldest = lru.entrySet().iterator().next();
                lru.remove(eldest.getKey());
                database.save(eldest.getValue());
                log.info("Evicted LRU -> Postgres: {}", eldest.getValue());
            }
        } finally {
            lock.unlock();
        }
    }
    /**
     * Remove from cache and Postgres (delete-through).
     * - If the id is NOT in cache: throw 404 (ResourceNotFoundException).
     * - If the id is in cache: remove from cache, then attempt DB delete.
     *   If the row isn't in DB already, treat as no-op (idempotent delete).
     */
    public void remove(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be null/blank");
        }

        boolean removedFromCache = false;
        lock.lock();
        try {
            // Try remove from cache first
            EntityModel removed = lru.remove(id);
            removedFromCache = (removed != null);
        } finally {
            lock.unlock();
        }

        if (removedFromCache) {
            log.info("Removed from cache: {}", id);

            // Delete in DB (idempotent - ignore if not present)
            try {
                database.delete(id);
                log.info("Removed from Postgres: {}", id);
            } catch (EmptyResultDataAccessException ex) {
                log.warn("Entity {} not found in Postgres during delete (no-op)", id);
            }
            return; // success
        }

        // Not in cache → check DB
        log.info("Entity {} not found in cache; checking Postgres...", id);

        // If present in DB, delete it and return success
        if (database.findById(id).isPresent()) {
            try {
                database.delete(id);
                log.info("Entity {} was not in cache but was deleted from Postgres", id);
                return; // success
            } catch (EmptyResultDataAccessException ex) {
                // Very unlikely (race), treat as success
                log.warn("Entity {} vanished from Postgres during delete (no-op)", id);
                return;
            }
        }

        // Not in cache AND not in DB -> 404
        throw new ResourceNotFoundException(
                "Entity with id=" + id + " not found in cache or Postgres"
        );
    }
    /** remove all from cache and DB */
    public void removeAll() {
        lock.lock();
        try { lru.clear(); log.info("Cleared entire cache"); }
        finally { lock.unlock(); }
        database.deleteAll();
        log.info("Cleared entire Postgres table");
    }

    /** get from cache; if miss, load from DB and re-cache; else 404 */
    public Optional<EntityModel> get(String id) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("id must not be null/blank");

        lock.lock();
        try {
            EntityModel hit = lru.get(id);
            if (hit != null) {
                log.info("Cache HIT id={}", id);
                return Optional.of(hit);
            }
        } finally {
            lock.unlock();
        }

        log.info("Cache MISS id={}, checking Postgres", id);
        Optional<EntityModel> fromDb = database.findById(id);
        if (!fromDb.isPresent())
            throw new ResourceNotFoundException("Entity with id=" + id + " not found in cache or Postgres");

        add(fromDb.get()); // re-cache
        return fromDb;
    }

    /** clear cache only (DB untouched) */
    public void clear() {
        lock.lock();
        try { lru.clear(); log.info("Cleared cache only"); }
        finally { lock.unlock(); }
    }

    /** visible for tests */
    public int cacheSize() {
        lock.lock();
        try { return lru.size(); }
        finally { lock.unlock(); }
    }
}
