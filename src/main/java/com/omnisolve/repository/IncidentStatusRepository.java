package com.omnisolve.repository;

import com.omnisolve.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentStatusRepository extends JpaRepository<IncidentStatus, Long> {
    Optional<IncidentStatus> findByName(String name);
}
