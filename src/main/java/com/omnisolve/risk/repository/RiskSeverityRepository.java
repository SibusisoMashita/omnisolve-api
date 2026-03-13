package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.RiskSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskSeverityRepository extends JpaRepository<RiskSeverity, Long> {
}
