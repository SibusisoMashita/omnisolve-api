package com.omnisolve.contractor.repository;

import com.omnisolve.contractor.domain.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractorRepository extends JpaRepository<Contractor, UUID> {

    List<Contractor> findByOrganisationId(Long organisationId);

    Optional<Contractor> findByIdAndOrganisationId(UUID id, Long organisationId);

    boolean existsByIdAndOrganisationId(UUID id, Long organisationId);

    @Query(value = """
            SELECT
                c.id::text                        AS contractor_id,
                c.name                            AS contractor_name,
                COUNT(DISTINCT w.id)              AS workers,
                COUNT(DISTINCT dt.id)             AS required_documents,
                COUNT(DISTINCT d.id) FILTER (
                    WHERE d.expiry_date IS NULL OR d.expiry_date > CURRENT_DATE
                )                                 AS valid_documents,
                COUNT(DISTINCT d.id) FILTER (
                    WHERE d.expiry_date < CURRENT_DATE
                )                                 AS expired_documents,
                COUNT(DISTINCT d.id) FILTER (
                    WHERE d.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
                )                                 AS expiring_documents,
                COUNT(DISTINCT d.document_type_id) AS covered_documents
            FROM contractors c
            LEFT JOIN contractor_workers w
                ON w.contractor_id = c.id
            CROSS JOIN contractor_document_types dt
            LEFT JOIN contractor_documents d
                ON d.contractor_id = c.id
                AND d.document_type_id = dt.id
            WHERE c.organisation_id = :organisationId
            GROUP BY c.id, c.name
            """, nativeQuery = true)
    List<ContractorComplianceProjection> findComplianceByOrganisationId(@Param("organisationId") Long organisationId);
}
