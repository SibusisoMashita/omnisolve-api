package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskCategoryRepository extends JpaRepository<RiskCategory, Long> {
}
