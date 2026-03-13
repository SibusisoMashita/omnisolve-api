package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.InspectionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionAttachmentRepository extends JpaRepository<InspectionAttachment, Long> {

    List<InspectionAttachment> findByInspectionIdOrderByUploadedAtDesc(UUID inspectionId);
}
