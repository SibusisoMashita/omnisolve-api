package com.omnisolve.repository;

import com.omnisolve.domain.Document;
import com.omnisolve.domain.DocumentVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    Optional<DocumentVersion> findTopByDocumentOrderByVersionNumberDesc(Document document);
}

