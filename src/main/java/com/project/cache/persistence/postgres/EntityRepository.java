package com.project.cache.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for JpaEntityRecord. */
public interface EntityRepository extends JpaRepository<JpaEntityRecord, Long> {
}
