package com.omnisolve.repository;

import com.omnisolve.domain.IncidentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentAttachmentRepository extends JpaRepository<IncidentAttachment, Long> {
    List<IncidentAttachment> findByIncidentIdOrderByUploadedAtDesc(UUID incidentId);
}
