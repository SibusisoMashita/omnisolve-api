package com.omnisolve.risk.service;

import com.omnisolve.audit.Auditable;
import com.omnisolve.risk.domain.Risk;
import com.omnisolve.risk.domain.RiskAttachment;
import com.omnisolve.risk.repository.RiskAttachmentRepository;
import com.omnisolve.risk.repository.RiskRepository;
import com.omnisolve.risk.service.dto.RiskAttachmentDTO;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAttachmentService.class);

    private final RiskAttachmentRepository attachmentRepository;
    private final RiskRepository riskRepository;
    private final S3StorageService s3StorageService;
    private final SecurityContextFacade securityContextFacade;

    public RiskAttachmentService(
            RiskAttachmentRepository attachmentRepository,
            RiskRepository riskRepository,
            S3StorageService s3StorageService,
            SecurityContextFacade securityContextFacade) {
        this.attachmentRepository = attachmentRepository;
        this.riskRepository = riskRepository;
        this.s3StorageService = s3StorageService;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<RiskAttachmentDTO> listAttachments(UUID riskId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        findRisk(riskId, organisationId);
        return attachmentRepository.findByRiskIdOrderByUploadedAtDesc(riskId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @Auditable(action = "RISK_ATTACHMENT_UPLOADED", entityType = "risk")
    public RiskAttachmentDTO uploadAttachment(UUID riskId, MultipartFile file, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Uploading attachment: riskId={}, filename={}, user={}", riskId, file.getOriginalFilename(), userId);

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }
        if (file.getSize() > 50 * 1024 * 1024L) {
            throw new ResponseStatusException(BAD_REQUEST, "File size exceeds maximum allowed size of 50 MB");
        }

        Risk risk = findRisk(riskId, organisationId);

        String safeFileName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        String s3Key = "risks/" + riskId + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
        } catch (IOException ex) {
            log.error("S3 upload failed: riskId={}, error={}", riskId, ex.getMessage(), ex);
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        RiskAttachment attachment = new RiskAttachment();
        attachment.setRisk(risk);
        attachment.setS3Key(s3Key);
        attachment.setFileName(safeFileName);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploadedBy(userId);
        attachment.setUploadedAt(OffsetDateTime.now());

        return toDTO(attachmentRepository.save(attachment));
    }

    @Transactional
    @Auditable(action = "RISK_ATTACHMENT_DELETED", entityType = "risk")
    public void deleteAttachment(Long attachmentId) {
        log.info("Deleting risk attachment: attachmentId={}", attachmentId);

        RiskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk attachment not found"));
        attachmentRepository.delete(attachment);
    }

    private Risk findRisk(UUID riskId, Long organisationId) {
        return riskRepository.findByIdAndOrganisationId(riskId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk not found"));
    }

    private RiskAttachmentDTO toDTO(RiskAttachment a) {
        return new RiskAttachmentDTO(a.getId(), a.getRisk().getId(), a.getFileName(),
                a.getFileSize(), a.getMimeType(), a.getS3Key(), a.getUploadedBy(), a.getUploadedAt());
    }
}
