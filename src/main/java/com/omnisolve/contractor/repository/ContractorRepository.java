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
                c.id::text            AS contractor_id,
                c.name                AS name,
                (SELECT COUNT(*) FROM contractor_document_types) AS required_documents,
                COUNT(DISTINCT CASE
                    WHEN cd.expiry_date IS NULL OR cd.expiry_date > CURRENT_DATE + INTERVAL '30 days'
                    THEN cd.document_type_id END)                AS valid_documents,
                COUNT(DISTINCT CASE
                    WHEN cd.expiry_date IS NOT NULL
                     AND cd.expiry_date > CURRENT_DATE
                     AND cd.expiry_date <= CURRENT_DATE + INTERVAL '30 days'
                    THEN cd.document_type_id END)                AS expiring_documents,
                COUNT(DISTINCT CASE
                    WHEN cd.expiry_date IS NOT NULL
                     AND cd.expiry_date < CURRENT_DATE
                    THEN cd.document_type_id END)                AS expired_documents,
                GREATEST(0,
                    (SELECT COUNT(*) FROM contractor_document_types)
                    - COUNT(DISTINCT cd.document_type_id))       AS missing_documents
            FROM contractors c
            LEFT JOIN contractor_documents cd ON cd.contractor_id = c.id
            WHERE c.organisation_id = :organisationId
            GROUP BY c.id, c.name
            """, nativeQuery = true)
    List<ContractorComplianceProjection> findComplianceByOrganisationId(@Param("organisationId") Long organisationId);
}
