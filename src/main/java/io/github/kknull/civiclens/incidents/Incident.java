package io.github.kknull.civiclens.incidents;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * Persistent representation of a reported municipal incident.
 *
 * <p>The database schema is managed by Flyway; this entity maps the Java
 * persistence model onto the existing {@code incidents} table.</p>
 */
@Entity
@Table(name = "incidents")
public class Incident {

    // Database-managed persistence fields.
    // PostgreSQL generates both values when an incident is inserted.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(name = "imported_at",
            nullable = false,
            insertable = false,
            updatable = false)
    private Instant importedAt;


    // Required source fields.
    // These values must be present for every persisted incident.
    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(name = "source_record_id", nullable = false)
    private String sourceRecordId;

    @Column(name = "source_offense_code", nullable = false)
    private String sourceOffenseCode;

    @Column(name = "source_offense_description", nullable = false)
    private String sourceOffenseDescription;

    @Column(name = "family_violence", nullable = false)
    private boolean familyViolence;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    // Optional source metadata.
    // These fields may be null when the upstream dataset does not provide a value.
    @Column(name = "location_type")
    private String locationType;

    @Column(name = "council_district")
    private Integer councilDistrict;

    @Column(name = "apd_sector")
    private String apdSector;

    @Column(name = "apd_district")
    private String apdDistrict;

    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_status")
    private ClearanceStatus clearanceStatus;

    @Column(name = "clearance_date")
    private LocalDate clearanceDate;

    @Column(name = "ucr_category")
    private String ucrCategory;

    @Column(name = "category_description")
    private String categoryDescription;

    @Column(name = "census_block_group")
    private String censusBlockGroup;

    // Required by JPA so Hibernate can instantiate entities when loading rows.
    protected Incident() {
    }

    // Creates the minimum valid incident owned by application code.
    // Database-generated and optional fields are intentionally excluded.
    public Incident(String sourceSystem, String sourceRecordId, String sourceOffenseCode,
            String sourceOffenseDescription, boolean familyViolence, LocalDateTime occurredAt,
            LocalDateTime reportedAt) {

        this.sourceSystem = Objects.requireNonNull(sourceSystem);
        this.sourceRecordId = Objects.requireNonNull(sourceRecordId);
        this.sourceOffenseCode = Objects.requireNonNull(sourceOffenseCode);
        this.sourceOffenseDescription = Objects.requireNonNull(sourceOffenseDescription);
        this.familyViolence = familyViolence;
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.reportedAt = Objects.requireNonNull(reportedAt);
    }

    // Optional-field mutators.
    // Required identity/source fields are intentionally not freely mutable.
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public void setCouncilDistrict(Integer councilDistrict) {
        this.councilDistrict = councilDistrict;
    }

    public void setApdSector(String apdSector) {
        this.apdSector = apdSector;
    }

    public void setApdDistrict(String apdDistrict) {
        this.apdDistrict = apdDistrict;
    }

    public void setClearanceStatus(ClearanceStatus clearanceStatus) {
        this.clearanceStatus = clearanceStatus;
    }

    public void setClearanceDate(LocalDate clearanceDate) {
        this.clearanceDate = clearanceDate;
    }

    public void setUcrCategory(String ucrCategory) {
        this.ucrCategory = ucrCategory;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public void setCensusBlockGroup(String censusBlockGroup) {
        this.censusBlockGroup = censusBlockGroup;
    }

    // Read access to persisted incident state.
    public Long getId() {
        return id;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public String getSourceOffenseCode() {
        return sourceOffenseCode;
    }

    public String getSourceOffenseDescription() {
        return sourceOffenseDescription;
    }

    public boolean isFamilyViolence() {
        return familyViolence;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public String getLocationType() {
        return locationType;
    }

    public Integer getCouncilDistrict() {
        return councilDistrict;
    }

    public String getApdSector() {
        return apdSector;
    }

    public String getApdDistrict() {
        return apdDistrict;
    }

    public ClearanceStatus getClearanceStatus() {
        return clearanceStatus;
    }

    public LocalDate getClearanceDate() {
        return clearanceDate;
    }

    public String getUcrCategory() {
        return ucrCategory;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public String getCensusBlockGroup() {
        return censusBlockGroup;
    }
}
