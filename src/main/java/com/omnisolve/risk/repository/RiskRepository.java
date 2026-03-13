package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.Risk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RiskRepository extends JpaRepository<Risk, UUID> {

    Optional<Risk> findByIdAndOrganisationId(UUID id, Long organisationId);

    @Query("SELECT r FROM Risk r WHERE r.organisation.id = :organisationId " +
           "AND (:categoryId IS NULL OR r.category.id = :categoryId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (COALESCE(:search, '') = '' OR r.title LIKE CONCAT('%', :search, '%'))")
    Page<Risk> findByOrganisationIdWithFilters(
            @Param("organisationId") Long organisationId,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM Risk r WHERE r.organisation.id = :organisationId AND r.riskScore >= 12")
    long countHighByOrganisationId(@Param("organisationId") Long organisationId);

    @Query("SELECT COUNT(r) FROM Risk r WHERE r.organisation.id = :organisationId AND r.riskScore BETWEEN 6 AND 11")
    long countMediumByOrganisationId(@Param("organisationId") Long organisationId);

    @Query("SELECT COUNT(r) FROM Risk r WHERE r.organisation.id = :organisationId AND r.riskScore <= 5")
    long countLowByOrganisationId(@Param("organisationId") Long organisationId);

    @Query("SELECT COUNT(r) FROM Risk r WHERE r.organisation.id = :organisationId " +
           "AND r.reviewDate < :today AND r.status != 'CLOSED'")
    long countOverdueReviewsByOrganisationId(
            @Param("organisationId") Long organisationId,
            @Param("today") LocalDate today
    );
}
