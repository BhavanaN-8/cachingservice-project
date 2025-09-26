package com.project.cache.persistence;

import com.project.cache.domain.EntityModel;
import java.util.Optional;

/** Abstraction for DB access so the service isn't tied to Postgres. */
public interface PersistencePort {
    void save(EntityModel entity);
    void delete(String id);
    void deleteAll();
    Optional<EntityModel> findById(String id);
}
