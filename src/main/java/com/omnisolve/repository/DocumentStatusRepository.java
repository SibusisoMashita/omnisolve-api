package com.omnisolve.repository;

import com.omnisolve.domain.DocumentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentStatusRepository extends JpaRepository<DocumentStatus, Long> {

    Optional<DocumentStatus> findByName(String name);
}

