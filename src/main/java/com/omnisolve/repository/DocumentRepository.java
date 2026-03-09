package com.omnisolve.repository;

import com.omnisolve.domain.Document;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}

