package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionFindingRepository extends JpaRepository<InspectionFinding, Long> {

    List<InspectionFinding> findByInspectionIdOrderByCreatedAtDesc(UUID inspectionId);
}
