package com.omnisolve.risk.service;

import com.omnisolve.audit.Auditable;
import com.omnisolve.config.CacheConfig;
import com.omnisolve.domain.Organisation;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.risk.domain.Risk;
import com.omnisolve.risk.domain.RiskAttachment;
import com.omnisolve.risk.domain.RiskCategory;
import com.omnisolve.risk.domain.RiskControl;
import com.omnisolve.risk.domain.RiskLikelihood;
import com.omnisolve.risk.domain.RiskSeverity;
import com.omnisolve.risk.repository.RiskAttachmentRepository;
import com.omnisolve.risk.repository.RiskCategoryRepository;
import com.omnisolve.risk.repository.RiskControlRepository;
import com.omnisolve.risk.repository.RiskLikelihoodRepository;
import com.omnisolve.risk.repository.RiskRepository;
import com.omnisolve.risk.repository.RiskSeverityRepository;
import com.omnisolve.risk.service.dto.RiskAttachmentDTO;
import com.omnisolve.risk.service.dto.RiskControlDTO;
import com.omnisolve.risk.service.dto.RiskCreateRequest;
import com.omnisolve.risk.service.dto.RiskDTO;
import com.omnisolve.risk.service.dto.RiskDashboardResponse;
import com.omnisolve.risk.service.dto.RiskUpdateRequest;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final RiskRepository riskRepository;
    private final RiskCategoryRepository categoryRepository;
    private final RiskSeverityRepository severityRepository;
    private final RiskLikelihoodRepository likelihoodRepository;
    private final RiskControlRepository controlRepository;
    private final RiskAttachmentRepository attachmentRepository;
    private final OrganisationRepository organisationRepository;
    private final SecurityContextFacade securityContextFacade;

    public RiskService(
            RiskRepository riskRepository,
            RiskCategoryRepository categoryRepository,
            RiskSeverityRepository severityRepository,
            RiskLikelihoodRepository likelihoodRepository,
            RiskControlRepository controlRepository,
            RiskAttachmentRepository attachmentRepository,
            OrganisationRepository organisationRepository,
            SecurityContextFacade securityContextFacade) {
        this.riskRepository = riskRepository;
        this.categoryRepository = categoryRepository;
        this.severityRepository = severityRepository;
        this.likelihoodRepository = likelihoodRepository;
        this.controlRepository = controlRepository;
        this.attachmentRepository = attachmentRepository;
        this.organisationRepository = organisationRepository;
        this.securityContextFacade = securityContextFacade;
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<RiskDTO> listRisks(Long categoryId, String status, String search, Pageable pageable) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Listing risks: organisationId={}", organisationId);
        return riskRepository
                .findByOrganisationIdWithFilters(organisationId, categoryId, status, search, pageable)
                .map(r -> toDTO(r, List.of(), List.of()));
    }

    @Transactional(readOnly = true)
    public RiskDTO getRisk(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching risk: id={}, organisationId={}", id, organisationId);

        Risk risk = findRisk(id, organisationId);

        List<RiskControlDTO> controls = controlRepository
                .findByRiskIdOrderByCreatedAtAsc(id).stream()
                .map(this::toControlDTO).toList();

        List<RiskAttachmentDTO> attachments = attachmentRepository
                .findByRiskIdOrderByUploadedAtDesc(id).stream()
                .map(this::toAttachmentDTO).toList();

        return toDTO(risk, controls, attachments);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.RISK_DASHBOARD, key = "#root.target.currentOrgId()")
    public RiskDashboardResponse getDashboard() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Calculating risk dashboard: organisationId={}", organisationId);

        long high = riskRepository.countHighByOrganisationId(organisationId);
        long medium = riskRepository.countMediumByOrganisationId(organisationId);
        long low = riskRepository.countLowByOrganisationId(organisationId);
        long overdue = riskRepository.countOverdueReviewsByOrganisationId(organisationId, LocalDate.now());

        return new RiskDashboardResponse(high + medium + low, high, medium, low, overdue);
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    @Transactional
    @Auditable(action = "RISK_CREATED", entityType = "risk")
    @CacheEvict(value = CacheConfig.RISK_DASHBOARD, key = "#root.target.currentOrgId()")
    public RiskDTO createRisk(RiskCreateRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Creating risk: title={}, organisationId={}", request.title(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));

        RiskCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk category not found"));

        RiskSeverity severity = severityRepository.findById(request.severityId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk severity not found"));

        RiskLikelihood likelihood = likelihoodRepository.findById(request.likelihoodId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk likelihood not found"));

        int riskScore = severity.getLevel() * likelihood.getLevel();

        OffsetDateTime now = OffsetDateTime.now();

        Risk risk = new Risk();
        risk.setOrganisation(organisation);
        risk.setTitle(request.title());
        risk.setDescription(request.description());
        risk.setCategory(category);
        risk.setSeverity(severity);
        risk.setLikelihood(likelihood);
        risk.setRiskScore(riskScore);
        risk.setOwnerId(request.ownerId());
        risk.setStatus("OPEN");
        risk.setReviewDate(request.reviewDate());
        risk.setIdentifiedAt(now);
        risk.setCreatedAt(now);
        risk.setUpdatedAt(now);

        Risk saved = riskRepository.save(risk);
        log.info("Risk created: id={}, riskScore={}, organisationId={}", saved.getId(), riskScore, organisationId);

        return toDTO(saved, List.of(), List.of());
    }

    @Transactional
    @Auditable(action = "RISK_UPDATED", entityType = "risk")
    @CacheEvict(value = CacheConfig.RISK_DASHBOARD, key = "#root.target.currentOrgId()")
    public RiskDTO updateRisk(UUID id, RiskUpdateRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Updating risk: id={}, organisationId={}", id, organisationId);

        Risk risk = findRisk(id, organisationId);

        if (request.title() != null)       risk.setTitle(request.title());
        if (request.description() != null)  risk.setDescription(request.description());
        if (request.ownerId() != null)      risk.setOwnerId(request.ownerId());
        if (request.status() != null)       risk.setStatus(request.status());
        if (request.reviewDate() != null)   risk.setReviewDate(request.reviewDate());

        if (request.categoryId() != null) {
            risk.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk category not found")));
        }

        boolean scoreChanged = false;
        if (request.severityId() != null) {
            risk.setSeverity(severityRepository.findById(request.severityId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk severity not found")));
            scoreChanged = true;
        }
        if (request.likelihoodId() != null) {
            risk.setLikelihood(likelihoodRepository.findById(request.likelihoodId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk likelihood not found")));
            scoreChanged = true;
        }

        if (scoreChanged && risk.getSeverity() != null && risk.getLikelihood() != null) {
            risk.setRiskScore(risk.getSeverity().getLevel() * risk.getLikelihood().getLevel());
        }

        risk.setUpdatedAt(OffsetDateTime.now());
        Risk saved = riskRepository.save(risk);
        log.info("Risk updated: id={}, riskScore={}", saved.getId(), saved.getRiskScore());

        return toDTO(saved, List.of(), List.of());
    }

    @Transactional
    @Auditable(action = "RISK_DELETED", entityType = "risk")
    @CacheEvict(value = CacheConfig.RISK_DASHBOARD, key = "#root.target.currentOrgId()")
    public void deleteRisk(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Deleting risk: id={}, organisationId={}", id, organisationId);

        Risk risk = findRisk(id, organisationId);
        riskRepository.delete(risk);
        log.info("Risk deleted: id={}", id);
    }

    // =========================================================================
    // SpEL helper for @Cacheable key expressions
    // =========================================================================

    public Long currentOrgId() {
        Long fromContext = TenantContext.getOrganisationId();
        return fromContext != null ? fromContext : securityContextFacade.currentUser().organisationId();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Risk findRisk(UUID id, Long organisationId) {
        return riskRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk not found"));
    }

    RiskDTO toDTO(Risk risk, List<RiskControlDTO> controls, List<RiskAttachmentDTO> attachments) {
        return new RiskDTO(
                risk.getId(),
                risk.getTitle(),
                risk.getDescription(),
                risk.getCategory() != null ? risk.getCategory().getId() : null,
                risk.getCategory() != null ? risk.getCategory().getName() : null,
                risk.getSeverity() != null ? risk.getSeverity().getId() : null,
                risk.getSeverity() != null ? risk.getSeverity().getName() : null,
                risk.getLikelihood() != null ? risk.getLikelihood().getId() : null,
                risk.getLikelihood() != null ? risk.getLikelihood().getName() : null,
                risk.getRiskScore(),
                risk.getStatus(),
                risk.getOwnerId(),
                risk.getReviewDate(),
                risk.getIdentifiedAt(),
                risk.getCreatedAt(),
                risk.getUpdatedAt(),
                controls,
                attachments
        );
    }

    RiskControlDTO toControlDTO(RiskControl c) {
        return new RiskControlDTO(c.getId(), c.getRisk().getId(), c.getDescription(),
                c.getControlOwner(), c.getCreatedAt());
    }

    RiskAttachmentDTO toAttachmentDTO(RiskAttachment a) {
        return new RiskAttachmentDTO(a.getId(), a.getRisk().getId(), a.getFileName(),
                a.getFileSize(), a.getMimeType(), a.getS3Key(), a.getUploadedBy(), a.getUploadedAt());
    }
}
