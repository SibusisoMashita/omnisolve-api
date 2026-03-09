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

    long countByStatus(DocumentStatus status);

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
}

