package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionChecklistItemRepository extends JpaRepository<InspectionChecklistItem, Long> {

    List<InspectionChecklistItem> findByChecklistIdOrderBySortOrder(Long checklistId);
}
