package com.omnisolve.repository;

import com.omnisolve.domain.DocumentClauseLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentClauseLinkRepository extends JpaRepository<DocumentClauseLink, DocumentClauseLink.DocumentClauseLinkId> {

    @Query("SELECT dcl.clause.id FROM DocumentClauseLink dcl WHERE dcl.document.id = :documentId")
    List<Long> findClauseIdsByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT dcl FROM DocumentClauseLink dcl WHERE dcl.clause.id = :clauseId")
    List<DocumentClauseLink> findByClauseId(@Param("clauseId") Long clauseId);

    @Modifying
    @Query("DELETE FROM DocumentClauseLink dcl WHERE dcl.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}
