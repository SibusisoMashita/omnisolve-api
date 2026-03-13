package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.Inspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    Page<Inspection> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId, Pageable pageable);

    Optional<Inspection> findByIdAndOrganisationId(UUID id, Long organisationId);

    List<Inspection> findByAssetIdAndOrganisationIdOrderByCreatedAtDesc(UUID assetId, Long organisationId);

    long countByOrganisationId(Long organisationId);

    long countByOrganisationIdAndStatus(Long organisationId, String status);

    @Query("SELECT COUNT(i) FROM Inspection i WHERE i.organisation.id = :organisationId " +
           "AND i.scheduledAt < CURRENT_TIMESTAMP AND i.status = 'SCHEDULED'")
    long countOverdueByOrganisationId(@Param("organisationId") Long organisationId);

    @Query("SELECT i FROM Inspection i WHERE i.organisation.id = :organisationId " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (COALESCE(:search, '') = '' OR " +
           "     i.title LIKE CONCAT('%', :search, '%') OR " +
           "     i.inspectionNumber LIKE CONCAT('%', :search, '%'))")
    Page<Inspection> findByOrganisationIdWithFilters(
            @Param("organisationId") Long organisationId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
