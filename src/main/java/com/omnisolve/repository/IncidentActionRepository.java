package com.omnisolve.repository;

import com.omnisolve.domain.IncidentAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentActionRepository extends JpaRepository<IncidentAction, Long> {
    List<IncidentAction> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
