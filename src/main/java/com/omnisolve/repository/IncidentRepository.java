package com.omnisolve.repository;

import com.omnisolve.domain.Incident;
import com.omnisolve.domain.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIdAndOrganisationId(UUID id, Long organisationId);

    Page<Incident> findByOrganisationId(Long organisationId, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.organisation.id = :organisationId " +
           "AND (:statusId IS NULL OR i.status.id = :statusId) " +
           "AND (:severityId IS NULL OR i.severity.id = :severityId) " +
           "AND (:departmentId IS NULL OR i.department.id = :departmentId) " +
           "AND (:siteId IS NULL OR i.site.id = :siteId) " +
           "AND (COALESCE(:search, '') = '' OR " +
           "     i.title LIKE CONCAT('%', :search, '%') OR " +
           "     i.incidentNumber LIKE CONCAT('%', :search, '%'))")
    Page<Incident> findByOrganisationIdWithFilters(
            @Param("organisationId") Long organisationId,
            @Param("statusId") Long statusId,
            @Param("severityId") Long severityId,
            @Param("departmentId") Long departmentId,
            @Param("siteId") Long siteId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganisationId(Long organisationId);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.organisation.id = :organisationId AND i.status.name != 'Closed'")
    long countOpenByOrganisationId(@Param("organisationId") Long organisationId);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.organisation.id = :organisationId AND i.severity.severityLevel >= :level")
    long countHighSeverityByOrganisationId(@Param("organisationId") Long organisationId, @Param("level") Integer level);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (closed_at - created_at)) / 86400.0) FROM incidents " +
           "WHERE organisation_id = :organisationId AND closed_at IS NOT NULL", nativeQuery = true)
    Double calculateAverageClosureTimeDays(@Param("organisationId") Long organisationId);

    @Query("SELECT i FROM Incident i WHERE i.organisation.id = :organisationId AND i.type = :type " +
           "AND i.incidentNumber LIKE :pattern ORDER BY i.createdAt DESC")
    List<Incident> findByOrganisationAndTypeAndPattern(
            @Param("organisationId") Long organisationId,
            @Param("type") IncidentType type,
            @Param("pattern") String pattern
    );
}
