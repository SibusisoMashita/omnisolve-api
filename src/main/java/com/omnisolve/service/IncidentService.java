package com.omnisolve.service;

import com.omnisolve.audit.Auditable;
import com.omnisolve.config.CacheConfig;
import com.omnisolve.domain.Department;
import com.omnisolve.domain.Incident;
import com.omnisolve.domain.IncidentAction;
import com.omnisolve.domain.IncidentAttachment;
import com.omnisolve.domain.IncidentComment;
import com.omnisolve.domain.IncidentInvestigation;
import com.omnisolve.domain.IncidentSeverity;
import com.omnisolve.domain.IncidentStatus;
import com.omnisolve.domain.IncidentType;
import com.omnisolve.domain.Organisation;
import com.omnisolve.domain.Site;
import com.omnisolve.event.IncidentClosedEvent;
import com.omnisolve.event.IncidentCreatedEvent;
import com.omnisolve.event.IncidentStatusChangedEvent;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.repository.IncidentActionRepository;
import com.omnisolve.repository.IncidentAttachmentRepository;
import com.omnisolve.repository.IncidentCommentRepository;
import com.omnisolve.repository.IncidentInvestigationRepository;
import com.omnisolve.repository.IncidentRepository;
import com.omnisolve.repository.IncidentSeverityRepository;
import com.omnisolve.repository.IncidentStatusRepository;
import com.omnisolve.repository.IncidentTypeRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.repository.SiteRepository;
import com.omnisolve.security.AuthenticatedUser;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.dto.IncidentActionRequest;
import com.omnisolve.service.dto.IncidentActionResponse;
import com.omnisolve.service.dto.IncidentActionUpdateRequest;
import com.omnisolve.service.dto.IncidentAssignRequest;
import com.omnisolve.service.dto.IncidentAttachmentResponse;
import com.omnisolve.service.dto.IncidentCommentRequest;
import com.omnisolve.service.dto.IncidentCommentResponse;
import com.omnisolve.service.dto.IncidentDashboardResponse;
import com.omnisolve.service.dto.IncidentDetailResponse;
import com.omnisolve.service.dto.IncidentInvestigationRequest;
import com.omnisolve.service.dto.IncidentInvestigationResponse;
import com.omnisolve.service.dto.IncidentRequest;
import com.omnisolve.service.dto.IncidentResponse;
import com.omnisolve.service.dto.IncidentUpdateRequest;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Business logic for the Incident Management domain.
 *
 * <p>All write operations are wrapped in a database transaction, annotated with
 * {@link Auditable}, and followed by a domain event so that side-effects (audit
 * logging, notifications) are decoupled from the core write path.
 *
 * <p>Tenant isolation: every query that touches {@code incidents} includes
 * {@code organisationId} resolved via {@link SecurityContextFacade#currentUser()}.
 *
 * <p>Transaction notes:
 * <ul>
 *   <li>Incident creation + attachment upload are separate transactions so that a
 *       failed S3 upload does not roll back the incident record.</li>
 *   <li>Incident close sets both {@code status} and {@code closedAt} atomically.</li>
 *   <li>Adding investigation / actions / comments are independent transactions;
 *       each is a discrete user action.</li>
 * </ul>
 */
@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private static final String STATUS_OPEN   = "Reported";
    private static final String STATUS_CLOSED = "Closed";

    private final IncidentRepository incidentRepository;
    private final IncidentTypeRepository incidentTypeRepository;
    private final IncidentSeverityRepository incidentSeverityRepository;
    private final IncidentStatusRepository incidentStatusRepository;
    private final IncidentAttachmentRepository incidentAttachmentRepository;
    private final IncidentInvestigationRepository incidentInvestigationRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final DepartmentRepository departmentRepository;
    private final SiteRepository siteRepository;
    private final OrganisationRepository organisationRepository;
    private final S3StorageService s3StorageService;
    private final SecurityContextFacade securityContextFacade;
    private final ApplicationEventPublisher eventPublisher;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentTypeRepository incidentTypeRepository,
            IncidentSeverityRepository incidentSeverityRepository,
            IncidentStatusRepository incidentStatusRepository,
            IncidentAttachmentRepository incidentAttachmentRepository,
            IncidentInvestigationRepository incidentInvestigationRepository,
            IncidentActionRepository incidentActionRepository,
            IncidentCommentRepository incidentCommentRepository,
            DepartmentRepository departmentRepository,
            SiteRepository siteRepository,
            OrganisationRepository organisationRepository,
            S3StorageService s3StorageService,
            SecurityContextFacade securityContextFacade,
            ApplicationEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.incidentTypeRepository = incidentTypeRepository;
        this.incidentSeverityRepository = incidentSeverityRepository;
        this.incidentStatusRepository = incidentStatusRepository;
        this.incidentAttachmentRepository = incidentAttachmentRepository;
        this.incidentInvestigationRepository = incidentInvestigationRepository;
        this.incidentActionRepository = incidentActionRepository;
        this.incidentCommentRepository = incidentCommentRepository;
        this.departmentRepository = departmentRepository;
        this.siteRepository = siteRepository;
        this.organisationRepository = organisationRepository;
        this.s3StorageService = s3StorageService;
        this.securityContextFacade = securityContextFacade;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(
            Long statusId, Long severityId, Long departmentId, Long siteId,
            String search, Pageable pageable) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching incidents: organisationId={}", organisationId);
        return incidentRepository
                .findByOrganisationIdWithFilters(organisationId, statusId, severityId, departmentId, siteId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncident(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching incident detail: id={}, organisationId={}", id, organisationId);

        Incident incident = incidentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident not found"));

        List<IncidentAttachmentResponse> attachments = incidentAttachmentRepository
                .findByIncidentIdOrderByUploadedAtDesc(id).stream()
                .map(this::toAttachmentResponse).toList();

        IncidentInvestigationResponse investigation = incidentInvestigationRepository
                .findByIncidentId(id).map(this::toInvestigationResponse).orElse(null);

        List<IncidentActionResponse> actions = incidentActionRepository
                .findByIncidentIdOrderByCreatedAtDesc(id).stream()
                .map(this::toActionResponse).toList();

        List<IncidentCommentResponse> comments = incidentCommentRepository
                .findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(this::toCommentResponse).toList();

        return toDetailResponse(incident, attachments, investigation, actions, comments);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.INCIDENT_DASHBOARD, key = "#root.target.currentOrgId()")
    public IncidentDashboardResponse getDashboard() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Calculating incident dashboard: organisationId={}", organisationId);

        long total        = incidentRepository.countByOrganisationId(organisationId);
        long open         = incidentRepository.countOpenByOrganisationId(organisationId);
        long highSeverity = incidentRepository.countHighSeverityByOrganisationId(organisationId, 3);
        Double avgTime    = incidentRepository.calculateAverageClosureTimeDays(organisationId);

        return new IncidentDashboardResponse(total, open, highSeverity, avgTime != null ? avgTime : 0.0);
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    @Transactional
    @Auditable(action = "INCIDENT_CREATED", entityType = "INCIDENT")
    @CacheEvict(value = CacheConfig.INCIDENT_DASHBOARD, key = "#root.target.currentOrgId()")
    public IncidentResponse createIncident(IncidentRequest request, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        Long organisationId = user.organisationId();
        log.info("Creating incident: title={}, typeId={}, severityId={}, organisationId={}",
                request.title(), request.typeId(), request.severityId(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));
        IncidentType type = incidentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident type not found"));
        IncidentSeverity severity = incidentSeverityRepository.findById(request.severityId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident severity not found"));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));
        }
        Site site = null;
        if (request.siteId() != null) {
            site = siteRepository.findById(request.siteId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Site not found"));
        }

        String incidentNumber = generateIncidentNumber(type, organisationId);

        Incident incident = new Incident();
        incident.setOrganisation(organisation);
        incident.setIncidentNumber(incidentNumber);
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setType(type);
        incident.setSeverity(severity);
        incident.setStatus(findStatus(STATUS_OPEN));
        incident.setDepartment(department);
        incident.setSite(site);
        incident.setOccurredAt(request.occurredAt());
        incident.setReportedBy(userId);

        OffsetDateTime now = OffsetDateTime.now();
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);

        Incident saved = incidentRepository.save(incident);
        log.info("Incident created: id={}, incidentNumber={}, organisationId={}", saved.getId(), saved.getIncidentNumber(), organisationId);

        eventPublisher.publishEvent(new IncidentCreatedEvent(
                saved.getId(), saved.getIncidentNumber(),
                severity.getName(), type.getName(), organisationId, userId));

        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "INCIDENT_UPDATED", entityType = "INCIDENT")
    public IncidentResponse updateIncident(UUID id, IncidentUpdateRequest request, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Updating incident: id={}, organisationId={}", id, organisationId);

        Incident incident = findIncident(id, organisationId);

        if (request.title() != null)       incident.setTitle(request.title());
        if (request.description() != null)  incident.setDescription(request.description());
        if (request.severityId() != null) {
            incident.setSeverity(incidentSeverityRepository.findById(request.severityId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident severity not found")));
        }
        if (request.departmentId() != null) {
            incident.setDepartment(departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found")));
        }
        if (request.investigatorId() != null) incident.setInvestigatorId(request.investigatorId());

        incident.setUpdatedAt(OffsetDateTime.now());
        Incident saved = incidentRepository.save(incident);
        log.info("Incident updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "INCIDENT_STATUS_CHANGED", entityType = "INCIDENT")
    @CacheEvict(value = CacheConfig.INCIDENT_DASHBOARD, key = "#root.target.currentOrgId()")
    public IncidentResponse changeStatus(UUID id, Long statusId, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        log.info("Changing incident status: id={}, statusId={}", id, statusId);

        Incident incident = findIncident(id, user.organisationId());
        IncidentStatus status = incidentStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident status not found"));

        incident.setStatus(status);
        incident.setUpdatedAt(OffsetDateTime.now());
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publishEvent(new IncidentStatusChangedEvent(
                saved.getId(), saved.getIncidentNumber(), status.getName(), user.organisationId(), userId));

        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "INCIDENT_INVESTIGATOR_ASSIGNED", entityType = "INCIDENT")
    public IncidentResponse assignInvestigator(UUID id, String investigatorId, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Assigning investigator: id={}, investigatorId={}", id, investigatorId);

        Incident incident = findIncident(id, organisationId);
        incident.setInvestigatorId(investigatorId);
        incident.setUpdatedAt(OffsetDateTime.now());
        return toResponse(incidentRepository.save(incident));
    }

    @Transactional
    @Auditable(action = "INCIDENT_CLOSED", entityType = "INCIDENT")
    @CacheEvict(value = CacheConfig.INCIDENT_DASHBOARD, key = "#root.target.currentOrgId()")
    public IncidentResponse closeIncident(UUID id, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        log.info("Closing incident: id={}", id);

        Incident incident = findIncident(id, user.organisationId());
        incident.setStatus(findStatus(STATUS_CLOSED));
        OffsetDateTime now = OffsetDateTime.now();
        incident.setClosedAt(now);
        incident.setUpdatedAt(now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publishEvent(new IncidentClosedEvent(
                saved.getId(), saved.getIncidentNumber(), user.organisationId(), userId));

        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "INCIDENT_ATTACHMENT_UPLOADED", entityType = "INCIDENT")
    public IncidentAttachmentResponse uploadAttachment(UUID incidentId, MultipartFile file, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Upload attachment: incidentId={}, filename={}, user={}", incidentId, file.getOriginalFilename(), userId);

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }
        if (file.getSize() > 50 * 1024 * 1024L) {
            throw new ResponseStatusException(BAD_REQUEST, "File size exceeds maximum allowed size of 50 MB");
        }

        Incident incident = findIncident(incidentId, organisationId);

        String safeFileName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        String s3Key = "incidents/" + incidentId + "/" + UUID.randomUUID() + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
        } catch (IOException ex) {
            log.error("S3 upload failed: incidentId={}, error={}", incidentId, ex.getMessage(), ex);
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        IncidentAttachment attachment = new IncidentAttachment();
        attachment.setIncident(incident);
        attachment.setFileName(safeFileName);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setS3Key(s3Key);
        attachment.setUploadedBy(userId);
        attachment.setUploadedAt(OffsetDateTime.now());

        return toAttachmentResponse(incidentAttachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<IncidentAttachmentResponse> listAttachments(UUID incidentId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        findIncident(incidentId, organisationId);
        return incidentAttachmentRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId).stream()
                .map(this::toAttachmentResponse).toList();
    }

    @Transactional
    @Auditable(action = "INCIDENT_COMMENT_ADDED", entityType = "INCIDENT")
    public IncidentCommentResponse addComment(UUID incidentId, String comment, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Incident incident = findIncident(incidentId, organisationId);

        IncidentComment incidentComment = new IncidentComment();
        incidentComment.setIncident(incident);
        incidentComment.setComment(comment);
        incidentComment.setCreatedBy(userId);
        incidentComment.setCreatedAt(OffsetDateTime.now());

        return toCommentResponse(incidentCommentRepository.save(incidentComment));
    }

    @Transactional(readOnly = true)
    public List<IncidentCommentResponse> listComments(UUID incidentId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        findIncident(incidentId, organisationId);
        return incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(this::toCommentResponse).toList();
    }

    @Transactional
    @Auditable(action = "INCIDENT_INVESTIGATION_ADDED", entityType = "INCIDENT")
    public IncidentInvestigationResponse addInvestigation(UUID incidentId, IncidentInvestigationRequest request, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Incident incident = findIncident(incidentId, organisationId);

        IncidentInvestigation investigation = new IncidentInvestigation();
        investigation.setIncident(incident);
        investigation.setInvestigatorId(request.investigatorId());
        investigation.setAnalysisMethod(request.analysisMethod());
        investigation.setRootCause(request.rootCause());
        investigation.setFindings(request.findings());
        investigation.setCreatedAt(OffsetDateTime.now());

        return toInvestigationResponse(incidentInvestigationRepository.save(investigation));
    }

    @Transactional
    @Auditable(action = "INCIDENT_ACTION_ADDED", entityType = "INCIDENT")
    public IncidentActionResponse addAction(UUID incidentId, IncidentActionRequest request, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Incident incident = findIncident(incidentId, organisationId);

        IncidentAction action = new IncidentAction();
        action.setIncident(incident);
        action.setTitle(request.title());
        action.setDescription(request.description());
        action.setAssignedTo(request.assignedTo());
        action.setDueDate(request.dueDate());
        action.setStatus("Pending");
        action.setCreatedAt(OffsetDateTime.now());

        return toActionResponse(incidentActionRepository.save(action));
    }

    @Transactional
    @Auditable(action = "INCIDENT_ACTION_UPDATED", entityType = "INCIDENT")
    public IncidentActionResponse updateAction(Long actionId, IncidentActionUpdateRequest request, String userId) {
        log.info("Updating corrective action: actionId={}", actionId);
        IncidentAction action = incidentActionRepository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Corrective action not found"));

        if (request.status() != null)      action.setStatus(request.status());
        if (request.completedAt() != null)  action.setCompletedAt(request.completedAt());

        return toActionResponse(incidentActionRepository.save(action));
    }

    // =========================================================================
    // Helpers used by @Cacheable SpEL expressions
    // =========================================================================

    /**
     * SpEL-accessible helper so that cache key expressions can reference the
     * current organisation without performing a full DB call when the context
     * is already populated in {@link com.omnisolve.tenant.TenantContext}.
     */
    public Long currentOrgId() {
        Long fromContext = com.omnisolve.tenant.TenantContext.getOrganisationId();
        return fromContext != null ? fromContext : securityContextFacade.currentUser().organisationId();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Incident findIncident(UUID id, Long organisationId) {
        return incidentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Incident not found"));
    }

    private IncidentStatus findStatus(String name) {
        return incidentStatusRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Status not found: " + name));
    }

    private String generateIncidentNumber(IncidentType type, Long organisationId) {
        String prefix = type.getName().length() >= 3
                ? type.getName().substring(0, 3).toUpperCase()
                : type.getName().toUpperCase();
        int year = OffsetDateTime.now().getYear();
        String pattern = prefix + "-" + year + "-%";
        List<Incident> existing = incidentRepository.findByOrganisationAndTypeAndPattern(organisationId, type, pattern);

        int nextSequence = 1;
        for (Incident inc : existing) {
            String[] parts = inc.getIncidentNumber().split("-");
            if (parts.length == 3) {
                try {
                    int seq = Integer.parseInt(parts[2]);
                    if (seq >= nextSequence) nextSequence = seq + 1;
                } catch (NumberFormatException ignored) {
                    // Skip malformed numbers
                }
            }
        }
        return String.format("%s-%d-%03d", prefix, year, nextSequence);
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(), incident.getIncidentNumber(), incident.getTitle(), incident.getDescription(),
                incident.getType().getId(), incident.getType().getName(),
                incident.getSeverity().getId(), incident.getSeverity().getName(),
                incident.getStatus().getId(), incident.getStatus().getName(),
                incident.getDepartment() != null ? incident.getDepartment().getId() : null,
                incident.getDepartment() != null ? incident.getDepartment().getName() : null,
                incident.getSite() != null ? incident.getSite().getId() : null,
                incident.getSite() != null ? incident.getSite().getName() : null,
                incident.getInvestigatorId(), incident.getOccurredAt(), incident.getReportedBy(),
                incident.getClosedAt(), incident.getCreatedAt(), incident.getUpdatedAt());
    }

    private IncidentDetailResponse toDetailResponse(Incident incident,
            List<IncidentAttachmentResponse> attachments, IncidentInvestigationResponse investigation,
            List<IncidentActionResponse> actions, List<IncidentCommentResponse> comments) {
        return new IncidentDetailResponse(
                incident.getId(), incident.getIncidentNumber(), incident.getTitle(), incident.getDescription(),
                incident.getType().getId(), incident.getType().getName(),
                incident.getSeverity().getId(), incident.getSeverity().getName(),
                incident.getStatus().getId(), incident.getStatus().getName(),
                incident.getDepartment() != null ? incident.getDepartment().getId() : null,
                incident.getDepartment() != null ? incident.getDepartment().getName() : null,
                incident.getSite() != null ? incident.getSite().getId() : null,
                incident.getSite() != null ? incident.getSite().getName() : null,
                incident.getInvestigatorId(), incident.getOccurredAt(), incident.getReportedBy(),
                incident.getClosedAt(), incident.getCreatedAt(), incident.getUpdatedAt(),
                attachments, investigation, actions, comments);
    }

    private IncidentAttachmentResponse toAttachmentResponse(IncidentAttachment a) {
        return new IncidentAttachmentResponse(a.getId(), a.getIncident().getId(),
                a.getFileName(), a.getFileSize(), a.getMimeType(), a.getS3Key(),
                a.getUploadedBy(), a.getUploadedAt());
    }

    private IncidentInvestigationResponse toInvestigationResponse(IncidentInvestigation inv) {
        return new IncidentInvestigationResponse(inv.getId(), inv.getIncident().getId(),
                inv.getInvestigatorId(), inv.getAnalysisMethod(), inv.getRootCause(),
                inv.getFindings(), inv.getCreatedAt());
    }

    private IncidentActionResponse toActionResponse(IncidentAction a) {
        return new IncidentActionResponse(a.getId(), a.getIncident().getId(),
                a.getTitle(), a.getDescription(), a.getAssignedTo(), a.getDueDate(),
                a.getStatus(), a.getCompletedAt(), a.getCreatedAt());
    }

    private IncidentCommentResponse toCommentResponse(IncidentComment c) {
        return new IncidentCommentResponse(c.getId(), c.getIncident().getId(),
                c.getComment(), c.getCreatedBy(), c.getCreatedAt());
    }
}
