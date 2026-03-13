package com.omnisolve.repository;

import com.omnisolve.domain.IncidentSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentSeverityRepository extends JpaRepository<IncidentSeverity, Long> {
    Optional<IncidentSeverity> findByName(String name);
}
