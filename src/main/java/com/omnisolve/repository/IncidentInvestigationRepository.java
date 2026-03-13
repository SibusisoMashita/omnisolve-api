package com.omnisolve.repository;

import com.omnisolve.domain.IncidentInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentInvestigationRepository extends JpaRepository<IncidentInvestigation, Long> {
    Optional<IncidentInvestigation> findByIncidentId(UUID incidentId);
}
