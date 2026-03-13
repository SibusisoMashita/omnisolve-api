package com.omnisolve.repository;

import com.omnisolve.domain.Document;
import com.omnisolve.domain.DocumentStatus;
import com.omnisolve.domain.DocumentType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Organisation-scoped queries for multi-tenant security
    Optional<Document> findByIdAndOrganisationId(UUID id, Long organisationId);
    
    List<Document> findByOrganisationId(Long organisationId);
    
    List<Document> findByOrganisationIdAndStatusId(Long organisationId, Long statusId);
    
    List<Document> findByOrganisationIdAndDepartmentId(Long organisationId, Long departmentId);
    
    long countByOrganisationId(Long organisationId);
    
    long countByOrganisationIdAndStatusId(Long organisationId, Long statusId);

    // Legacy queries (should be updated to include organisation_id)
    long countByStatus(DocumentStatus status);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.organisation.id = :organisationId AND d.nextReviewAt <= :now AND d.status.name = 'Active'")
    long countReviewDueByOrganisation(@Param("organisationId") Long organisationId, @Param("now") OffsetDateTime now);

    @Query("SELECT d FROM Document d WHERE d.organisation.id = :organisationId AND d.status.name = 'Pending Approval' ORDER BY d.updatedAt ASC")
    List<Document> findPendingApprovalByOrganisation(@Param("organisationId") Long organisationId);

    @Query("SELECT d FROM Document d WHERE d.organisation.id = :organisationId AND d.status.name = 'Active' AND d.nextReviewAt <= :now ORDER BY d.nextReviewAt ASC")
    List<Document> findOverdueReviewsByOrganisation(@Param("organisationId") Long organisationId, @Param("now") OffsetDateTime now);

    @Query("SELECT d FROM Document d WHERE d.organisation.id = :organisationId AND d.status.name = 'Active' AND d.nextReviewAt BETWEEN :now AND :future ORDER BY d.nextReviewAt ASC")
    List<Document> findUpcomingReviewsByOrganisation(@Param("organisationId") Long organisationId, @Param("now") OffsetDateTime now, @Param("future") OffsetDateTime future);

    @Query("SELECT d FROM Document d WHERE d.organisation.id = :organisationId AND d.type = :type AND d.documentNumber LIKE :pattern ORDER BY d.createdAt DESC")
    List<Document> findByOrganisationAndTypeAndPattern(@Param("organisationId") Long organisationId, @Param("type") DocumentType type, @Param("pattern") String pattern);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.nextReviewAt <= :now AND d.status.name = 'Active'")
    long countReviewDue(OffsetDateTime now);

    @Query("SELECT d FROM Document d WHERE d.status.name = 'Pending Approval' ORDER BY d.updatedAt ASC")
    List<Document> findPendingApproval();

    @Query("SELECT d FROM Document d WHERE d.status.name = 'Active' AND d.nextReviewAt <= :now ORDER BY d.nextReviewAt ASC")
    List<Document> findOverdueReviews(@Param("now") OffsetDateTime now);

    @Query("SELECT d FROM Document d WHERE d.status.name = 'Active' AND d.nextReviewAt BETWEEN :now AND :future ORDER BY d.nextReviewAt ASC")
    List<Document> findUpcomingReviews(@Param("now") OffsetDateTime now, @Param("future") OffsetDateTime future);

    @Query("SELECT d FROM Document d WHERE d.type = :type AND d.documentNumber LIKE :pattern ORDER BY d.createdAt DESC")
    List<Document> findByTypeAndPattern(@Param("type") DocumentType type, @Param("pattern") String pattern);

    @Query(value = "SELECT d.* FROM documents d " +
           "INNER JOIN document_clause_links dcl ON d.id = dcl.document_id " +
           "WHERE dcl.clause_id = :clauseId " +
           "ORDER BY d.document_number ASC", 
           nativeQuery = true)
    List<Document> findByClauseId(@Param("clauseId") Long clauseId);
}

