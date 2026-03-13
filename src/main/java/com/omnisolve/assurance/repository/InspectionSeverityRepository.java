package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionSeverityRepository extends JpaRepository<InspectionSeverity, Long> {

    List<InspectionSeverity> findAllByOrderByLevelAsc();
}
