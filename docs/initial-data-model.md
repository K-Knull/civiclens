# Initial Data Model

## Status

**Draft — design validated against the Crime Reports 2025 dataset snapshot profiled on August 6, 2026. Implementation has not started.**

## Purpose

This document defines the first CivicLens domain model and records the evidence behind its design decisions. It is intended to guide the first database migration, ingestion pipeline, persistence layer, and REST API.

## Product Goal

CivicLens will support an equal combination of:

- Individual reported-incident exploration
- Aggregate trend and summary analysis

The first vertical slice will use Austin Police Department crime-report data for incidents that occurred in 2025.

## Source Dataset

- **Source:** City of Austin Crime Reports 2025
- **Dataset identifier:** `n376-8ah5`
- **Initial scope:** Incidents with occurrence dates in 2025
- **Profiled row count:** 89,119
- **API:** SODA3 JSON query endpoint
- **Source limitation:** Only the highest-level offense associated with an incident is represented

The 2025 view is scoped by occurrence date. A record may have a report date, clearance date, or later source update in 2026 or beyond. CivicLens must therefore avoid treating a completed calendar year as permanently static.

CivicLens will describe this dataset as reported-incident data. It will not present record counts as a complete measure of all crime, crime rates, or neighborhood safety.

## First User Questions

The first version should help users answer:

- What reported incidents occurred during a selected period?
- What source offense descriptions appear most frequently?
- How do reported-incident counts change over time?
- How do counts compare across council districts?
- Which incidents were marked as involving family violence?

## Design Principles

- Preserve meaningful source values before introducing CivicLens-owned classifications.
- Separate the raw Socrata representation from the normalized CivicLens domain model.
- Treat identifiers as strings unless they represent quantities used in arithmetic.
- Distinguish missing source values from explicit source categories such as `OTHER / UNKNOWN`.
- Do not infer undocumented meaning from source codes, identifier formats, or field names.
- Base database constraints on observed evidence while remaining resilient to future source changes.
- Prefer a simple first model that supports a complete vertical slice over premature normalization.

## Decision Summary

| Area | Decision |
|---|---|
| Record identity | Use a generated CivicLens primary key and retain the APD incident number as an external identifier |
| Source uniqueness | Enforce uniqueness across `sourceSystem` and `sourceRecordId` |
| Offense representation | Store source offense code and description directly on each incident |
| Temporal representation | Combine source date and time fields into `LocalDateTime` values |
| Public geography | Prioritize council district in the first API |
| Preserved geography | Retain council district, APD sector, and APD district |
| Family violence | Convert strict source values `Y` and `N` into a required Boolean |
| Clearance status | Use a nullable enum rather than a Boolean |
| Missing values | Preserve nulls instead of inventing replacement categories |
| Initial classification model | Do not create normalized offense or geography lookup tables yet |

## Domain Decisions

### Record Identity

CivicLens will use a generated internal `id` as the primary key for each incident.

The APD incident report number will be retained as `sourceRecordId` so that CivicLens records remain traceable to the original source. It will not serve as the database primary key because CivicLens should control its own identity model and remain resilient to changes or inconsistencies in external systems.

No duplicate non-null incident report numbers were found in the profiled 2025 snapshot. CivicLens will enforce uniqueness across:

```text
(sourceSystem, sourceRecordId)
```

This models external identity as unique within a specific source rather than assuming that an identifier is globally unique across every future CivicLens dataset.

No semantic meaning will be inferred from the incident-report-number format.

### Offense Representation

CivicLens will store both the source offense code and source offense description directly on each incident.

The initial model will not include a normalized offense table because profiling showed that one source offense code may map to multiple crime-type descriptions. A code therefore cannot safely serve as a unique lookup key for one canonical description.

The initial model will preserve:

- `sourceOffenseCode`
- `sourceOffenseDescription`
- Optional `ucrCategory`
- Optional `categoryDescription`

A separate CivicLens-owned canonical offense classification may be introduced later without replacing the original source values.

### Temporal Representation

CivicLens will store:

- `occurredAt`
- `reportedAt`

Both fields will use `LocalDateTime` in Java and `TIMESTAMP WITHOUT TIME ZONE` in PostgreSQL because the source provides Austin-local date and time values without a timezone offset.

The ingestion layer will combine the separate source date and numeric time fields. Numeric time values contain between one and four digits and will be left-padded to four digits before parsing.

Examples:

```text
0    -> 00:00
808  -> 08:08
1800 -> 18:00
```

Strict validation will reject or quarantine malformed future values rather than silently converting them.

### Geographic Representation

CivicLens will preserve:

- `councilDistrict`
- `apdSector`
- `apdDistrict`

Council district will be the primary public-facing geography in the first API because it is understandable in a civic context and supports district-level comparison.

APD sector and APD district will be stored as opaque source codes. They will not receive dedicated filtering or analytics features in the first release.

The initial model will not create separate geography lookup tables.

### Family-Violence Representation

The source contained only `Y` and `N` values for all profiled rows.

CivicLens will represent family violence as a required Boolean:

```text
Y -> true
N -> false
```

Any other value will be treated as a validation failure. Unexpected values must not be silently converted to `false`.

### Clearance Representation

Clearance status will be represented as a nullable enum with the observed meanings:

- `C` — Cleared by arrest
- `O` — Cleared by exception
- `N` — Not cleared

A missing status is distinct from the explicit `N` value.

`clearanceDate` will be documented as the source-provided date associated with the clearance status. It will not be described as necessarily representing the date an incident was solved because records with status `N` also contained a clearance date.

Both clearance fields will remain nullable. CivicLens will initially detect and report mismatched status/date pairs during ingestion rather than enforcing their relationship with a database constraint.

## Proposed Incident Fields

| Field | Java type | Required | Purpose |
|---|---|---:|---|
| `id` | `Long` | Yes | CivicLens-generated internal primary key |
| `sourceSystem` | `String` | Yes | External source identifier, initially `APD_CRIME_REPORTS` |
| `sourceRecordId` | `String` | Yes | APD incident report number |
| `sourceOffenseCode` | `String` | Yes | Source column `ucr_code` preserved as an opaque code |
| `sourceOffenseDescription` | `String` | Yes | Source-provided crime type |
| `familyViolence` | `boolean` | Yes | Strictly converted from source values `Y` and `N` |
| `occurredAt` | `LocalDateTime` | Yes | Combined source occurrence date and time |
| `reportedAt` | `LocalDateTime` | Yes | Combined source report date and time |
| `locationType` | `String` | No | Source-provided location classification |
| `councilDistrict` | `Integer` | No | Austin City Council district |
| `apdSector` | `String` | No | Opaque APD operational-sector code |
| `apdDistrict` | `String` | No | Opaque APD district code |
| `clearanceStatus` | `ClearanceStatus` | No | Cleared by arrest, cleared by exception, or not cleared |
| `clearanceDate` | `LocalDate` | No | Source-provided date associated with clearance status |
| `ucrCategory` | `String` | No | Optional standardized UCR category |
| `categoryDescription` | `String` | No | Optional UCR category description |
| `censusBlockGroup` | `String` | No | Census-geography identifier when provided |
| `importedAt` | `Instant` | Yes | Time the record was first imported into CivicLens |

## Nullability and Validation Rules

### Required source-derived fields

The profiled snapshot populated these fields for every row:

- Source record identifier
- Source offense code
- Source offense description
- Family-violence flag
- Occurrence date and time
- Report date and time

These values may become required database columns after the ingestion parser is implemented and tested against the full 2025 dataset.

### Nullable fields

The following fields must remain nullable:

- `locationType`
- `councilDistrict`
- `apdSector`
- `apdDistrict`
- `clearanceStatus`
- `clearanceDate`
- `ucrCategory`
- `categoryDescription`
- `censusBlockGroup`

Missing values will remain missing. CivicLens will not convert null geography or location values into `0`, an empty string, or an invented `UNKNOWN` category.

### Field-specific validation

- `familyViolence` accepts only `Y` or `N`.
- Source time values must contain one to four digits and parse to a valid hour and minute.
- Non-null council districts are expected to be between `1` and `10`.
- APD sector and district remain opaque strings because both include nonnumeric or special codes.
- Non-null census block groups matched a 10-digit format in the profiled snapshot.
- Clearance status/date mismatches will initially produce an ingestion warning or rejection for review rather than a database-level check constraint.

Observed formats are validation expectations for the first source, not universal guarantees for every future dataset or jurisdiction.

## Source-to-Domain Transformation

The raw SODA3 ingestion DTO will preserve source values primarily as strings because the API represents numeric-looking codes, district values, and time values as JSON strings.

Example:

```text
Socrata source DTO
occ_date = "2025-12-31T00:00:00"
occ_time = "808"
family_violence = "N"
council_district = "4"

            |
            v

CivicLens domain model
occurredAt = 2025-12-31T08:08
familyViolence = false
councilDistrict = 4
```

Conversion and validation belong in the ingestion layer rather than in the raw transport DTO.

## Initial API Scope

The first API will support both incident exploration and aggregate analysis.

### Incident exploration

Candidate endpoints:

```text
GET /api/incidents
GET /api/incidents/{id}
```

Initial capabilities:

- Paginated incident browsing
- Lookup by CivicLens internal ID
- Filtering by occurrence-date range
- Exact filtering by source offense code or source offense description
- Filtering by council district
- Filtering by family-violence status

### Analytics

Candidate endpoints:

```text
GET /api/analytics/summary
GET /api/analytics/incidents-over-time
GET /api/analytics/incidents-by-offense
GET /api/analytics/incidents-by-council-district
```

Initial analytics:

- Total reported incidents for a selected period
- Reported incidents grouped by month
- Reported incidents grouped by source offense
- Reported incidents grouped by council district

APD sector and APD district will be preserved in storage but will not receive dedicated filtering or analytics endpoints in the first release.

## Data Interpretation and Terminology

CivicLens will use terms such as:

- Reported incidents
- Incident records
- Records published by APD
- Reported-incident counts

CivicLens will not describe the first dataset as a complete measure of:

- All crime
- Crime rates
- Neighborhood safety
- Every police interaction or call for service

Any future rate-based analysis will require an explicit denominator, such as population, and a documented methodology.

## Deferred Scope

The following features are intentionally excluded from the first vertical slice:

- Historical data before 2025
- Incremental weekly synchronization
- Multiple municipal datasets
- Exact map visualization
- Full-text search
- A CivicLens-owned offense-classification system
- Clearance-status analytics
- APD sector and APD district analytics
- User accounts and saved searches
- Predictive policing or crime prediction
- Population-adjusted crime-rate calculations
- A public-facing dashboard

## Open Questions

- Can the Crime Reports API expose a reliable row-level update timestamp?
- Should initial ingestion use paginated SODA3 JSON requests or a bulk CSV export?
- Should source records be updated in place when APD republishes changed values?
- How should CivicLens record rejected or malformed source rows?
- Should import history be modeled as a separate ingestion-run entity?
- Which database indexes are justified by the first implemented queries?
- Should `sourceSystem` remain a string or become a CivicLens-owned enum?
- Does `importedAt` remain sufficient once updates are supported, or will the model need fields such as `lastObservedAt` or an ingestion-run relationship?

## Appendix A: Data-Profiling Evidence

### Snapshot summary

The Crime Reports 2025 dataset contained 89,119 rows when profiled.

No duplicate non-null incident report numbers were found.

### Completeness

Populated for all 89,119 rows:

- Incident report number
- Crime type
- UCR code
- Family-violence flag
- Occurrence date
- Occurrence time
- Report date
- Report time

Partially populated:

| Field | Populated rows | Missing rows |
|---|---:|---:|
| Council district | 88,342 | 777 |
| APD sector | 88,949 | 170 |
| APD district | 88,883 | 236 |
| UCR category | 34,233 | 54,886 |
| Category description | 34,233 | 54,886 |
| Location type | 89,107 | 12 |
| Census block group | 88,643 | 476 |

### Categorical values

Family violence:

| Value | Rows |
|---|---:|
| `N` | 82,647 |
| `Y` | 6,472 |

Clearance status:

| Value | Meaning | Rows |
|---|---|---:|
| `N` | Not cleared | 60,956 |
| `C` | Cleared by arrest | 18,758 |
| Missing | No source value | 9,307 |
| `O` | Cleared by exception | 98 |

Every row with a clearance status also contained a clearance date. Every row without a clearance status lacked a clearance date.

### Offense-code relationships

The snapshot contained 326 distinct offense-code and crime-type pairs.

Some source offense codes mapped to multiple crime-type descriptions. No crime-type description was observed with multiple source offense codes.

This is an observation about the current snapshot, not a permanent guarantee of the source system.

### Time values

- 1,440 distinct occurrence-time values
- 1,440 distinct report-time values
- 0 malformed or out-of-range occurrence times
- 0 malformed or out-of-range report times

The snapshot contained every possible minute value in a 24-hour day.

### Geographic values

Council district contained only values `1` through `10` when present.

APD sector included alphabetic, numeric-looking, and special codes such as:

```text
AD, AP, BA, BAKR, ID, IDA, UT, 88
```

APD district included numeric and alphanumeric values such as:

```text
1, 2, 3, 4, 5, 6, 7, 8, 88, A, B, B8, C, D, P, S
```

### Location type and census block group

- 46 distinct location-type groups were observed.
- Explicit `OTHER / UNKNOWN` values are distinct from missing values.
- 714 distinct populated census block-group identifiers were observed.
- Every populated census block group contained exactly 10 digits.
