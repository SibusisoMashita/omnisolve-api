package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.RiskLikelihood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskLikelihoodRepository extends JpaRepository<RiskLikelihood, Long> {
}
