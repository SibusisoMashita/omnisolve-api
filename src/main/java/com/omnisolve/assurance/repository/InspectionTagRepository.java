package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionTag;
import com.omnisolve.assurance.domain.InspectionTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionTagRepository extends JpaRepository<InspectionTag, InspectionTagId> {

    List<InspectionTag> findByInspectionId(UUID inspectionId);

    void deleteByInspectionId(UUID inspectionId);
}
