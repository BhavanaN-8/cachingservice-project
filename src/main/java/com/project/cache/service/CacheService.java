package com.project.cache.service;

import com.project.cache.domain.EntityModel;
import com.project.cache.exception.ResourceNotFoundException;
import com.project.cache.persistence.PersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

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

    /** remove from cache and DB */
    public void remove(String id) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("id must not be null/blank");

        lock.lock();
        try {
            if (!lru.containsKey(id)) {
                // If not in cache, throw 404
                throw new ResourceNotFoundException("Entity with id=" + id + " not found in cache");
            }

            lru.remove(id);
            log.info("Removed from cache: {}", id);
        } finally {
            lock.unlock();
        }
        try {
            database.delete(id);
        } catch (EmptyResultDataAccessException e){
            log.info("{} Not found in Postgres" , id);
        }

        log.info("Removed from Postgres: {}", id);
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
