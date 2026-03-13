package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionChecklistRepository extends JpaRepository<InspectionChecklist, Long> {

    List<InspectionChecklist> findByAssetTypeId(Long assetTypeId);
}
