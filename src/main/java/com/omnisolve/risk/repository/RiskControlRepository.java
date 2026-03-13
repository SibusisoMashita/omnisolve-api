package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.RiskControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskControlRepository extends JpaRepository<RiskControl, Long> {

    List<RiskControl> findByRiskIdOrderByCreatedAtAsc(UUID riskId);
}
