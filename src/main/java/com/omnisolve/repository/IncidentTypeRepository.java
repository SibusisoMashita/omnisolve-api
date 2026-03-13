package com.omnisolve.repository;

import com.omnisolve.domain.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentTypeRepository extends JpaRepository<IncidentType, Long> {
    Optional<IncidentType> findByName(String name);
}
