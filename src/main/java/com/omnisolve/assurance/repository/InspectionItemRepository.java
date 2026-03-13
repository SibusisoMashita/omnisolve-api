package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {

    List<InspectionItem> findByInspectionIdOrderById(UUID inspectionId);
}
