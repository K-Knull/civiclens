package io.github.kknull.civiclens.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for {@link Incident} entities.
 *
 * <p>Spring Data JPA generates the repository implementation at runtime.
 * Additional incident-specific queries can be declared here as they are needed.</p>
 */
public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
