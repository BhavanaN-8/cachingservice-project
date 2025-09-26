package com.project.cache.persistence.postgres;

import com.project.cache.domain.EntityModel;
import com.project.cache.persistence.PersistencePort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** JPA-based implementation of PersistencePort. */
@Service
public class PostgresDatabaseService implements PersistencePort {

    private final EntityRepository repo;

    public PostgresDatabaseService(EntityRepository repo){ this.repo = repo; }

    @Override
    public void save(EntityModel entity) {
        repo.save(new JpaEntityRecord(Long.valueOf(entity.getId()), entity.getData()));
    }

    @Override
    public void delete(String id) { repo.deleteById(Long.valueOf(id)); }

    @Override
    public void deleteAll() { repo.deleteAll(); }

    @Override
    public Optional<EntityModel> findById(String id) {
        return repo.findById(Long.valueOf(id))
                .map(r -> new EntityModel(String.valueOf(r.getId()), r.getData()));
    }
}
