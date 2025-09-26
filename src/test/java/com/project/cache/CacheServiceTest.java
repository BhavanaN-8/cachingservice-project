package com.project.cache;

import com.project.cache.domain.EntityModel;
import com.project.cache.exception.ResourceNotFoundException;
import com.project.cache.persistence.PersistencePort;
import com.project.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CacheServiceTest {

    private CacheService cache;
    private PersistencePort db;

    @BeforeEach
    void setup() {
        db = Mockito.mock(PersistencePort.class);
        cache = new CacheService(db, 3);
    }

    @Test
    void test_save_and_get() {
        cache.add(new EntityModel("1", "one"));
        assertTrue(cache.get("1").isPresent());
    }

    @Test
    void test_update_existing_entity() {
        cache.add(new EntityModel("1", "one"));
        cache.add(new EntityModel("1", "updated"));
        assertEquals("updated", cache.get("1").get().getData());
    }

    @Test
    void test_remove_existing_entity() {
        cache.add(new EntityModel("1", "one"));
        cache.remove("1");
        when(db.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cache.get("1"));
    }

    @Test
    void test_remove_nonexistent_entity() {
        assertThrows(ResourceNotFoundException.class,() -> cache.remove("99"));
    }

    @Test
    void test_remove_all_entities() {
        cache.add(new EntityModel("1", "one"));
        cache.add(new EntityModel("2", "two"));
        cache.removeAll();
        when(db.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cache.get("1"));
    }

    @Test
    void test_eviction_when_cache_full() {
        cache.add(new EntityModel("1", "one"));
        cache.add(new EntityModel("2", "two"));
        cache.add(new EntityModel("3", "three"));
        cache.add(new EntityModel("4", "four")); // eviction
        assertEquals(3, cache.cacheSize());
        verify(db, atLeastOnce()).save(any());
    }

    @Test
    void test_cache_hit() {
        cache.add(new EntityModel("1", "one"));
        Optional<EntityModel> first = cache.get("1");
        Optional<EntityModel> second = cache.get("1");
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        verify(db, never()).findById("1");
    }

    @Test
    void test_cache_miss_then_load_from_db() {
        when(db.findById("10")).thenReturn(Optional.of(new EntityModel("10", "ten")));
        Optional<EntityModel> result = cache.get("10");
        assertTrue(result.isPresent());
        assertEquals("ten", result.get().getData());
    }

    @Test
    void test_clear_cache_only() {
        cache.add(new EntityModel("1", "one"));
        cache.clear();
        when(db.findById("1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cache.get("1"));
    }

    @Test
    void test_cache_size_after_add() {
        cache.add(new EntityModel("1", "one"));
        cache.add(new EntityModel("2", "two"));
        cache.add(new EntityModel("3", "three"));
        assertEquals(3, cache.cacheSize());
    }

    @Test
    void test_get_nonexistent_throws() {
        when(db.findById("99")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cache.get("99"));
    }

    @Test
    void test_add_null_id_throws() {
        assertThrows(IllegalArgumentException.class, () -> cache.add(new EntityModel(null, "oops")));
    }

    @Test
    void test_db_exception_propagates() {
        doThrow(new RuntimeException("DB down")).when(db).save(any());
        cache = new CacheService(db, 1);
        cache.add(new EntityModel("1", "one"));
        assertThrows(RuntimeException.class, () -> cache.add(new EntityModel("2", "two"))); // eviction triggers db.save
    }

    @Test
    void test_remove_all_from_db() {
        cache.removeAll();
        verify(db, times(1)).deleteAll();
    }
}
