package io.github.kknull.civiclens.incidents;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Integration tests for the Incident persistence mapping and repository.
 *
 * <p>
 * These tests verify that Incident entities can be written to PostgreSQL
 * and loaded back through Spring Data JPA.
 * </p>
 */
@SpringBootTest
@Transactional
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsIncident() {
        Incident incident = new Incident(
                "APD_CRIME_REPORTS",
                "TEST-001",
                "600",
                "THEFT",
                false,
                LocalDateTime.of(2025, 6, 1, 12, 30),
                LocalDateTime.of(2025, 6, 1, 14, 0));

        incident.setCouncilDistrict(3);
        incident.setLocationType("RESIDENCE / HOME");
        incident.setClearanceStatus(ClearanceStatus.CLEARED_BY_ARREST);
        incident.setClearanceDate(LocalDate.of(2025, 6, 3));

        Incident saved = incidentRepository.saveAndFlush(incident);

        // Database-managed fields should be populated after the INSERT.
        assertNotNull(saved.getId());
        assertNotNull(saved.getImportedAt());

        Long incidentId = saved.getId();

        // Remove managed entities from the persistence context so findById()
        // must load the Incident from the database rather than reuse the cached instance.
        entityManager.clear();

        Incident loaded = incidentRepository.findById(incidentId).orElseThrow();

        // Verify the entity survived a complete Java -> PostgreSQL -> Java round trip.
        assertEquals("APD_CRIME_REPORTS", loaded.getSourceSystem());
        assertEquals("TEST-001", loaded.getSourceRecordId());
        assertEquals("600", loaded.getSourceOffenseCode());
        assertEquals("THEFT", loaded.getSourceOffenseDescription());
        assertEquals(3, loaded.getCouncilDistrict());
        assertEquals("RESIDENCE / HOME", loaded.getLocationType());
        assertEquals(ClearanceStatus.CLEARED_BY_ARREST, loaded.getClearanceStatus());
        assertEquals(LocalDate.of(2025, 6, 3), loaded.getClearanceDate());
        assertFalse(loaded.isFamilyViolence());
    }
}
