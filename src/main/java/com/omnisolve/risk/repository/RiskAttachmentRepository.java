package com.omnisolve.risk.repository;

import com.omnisolve.risk.domain.RiskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskAttachmentRepository extends JpaRepository<RiskAttachment, Long> {

    List<RiskAttachment> findByRiskIdOrderByUploadedAtDesc(UUID riskId);
}
