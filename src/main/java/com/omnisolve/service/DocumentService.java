package com.omnisolve.service;

import com.omnisolve.audit.Auditable;
import com.omnisolve.config.CacheConfig;
import com.omnisolve.domain.Department;
import com.omnisolve.domain.Document;
import com.omnisolve.domain.DocumentStatus;
import com.omnisolve.domain.DocumentType;
import com.omnisolve.domain.DocumentVersion;
import com.omnisolve.domain.Organisation;
import com.omnisolve.event.DocumentApprovedEvent;
import com.omnisolve.event.DocumentArchivedEvent;
import com.omnisolve.event.DocumentRejectedEvent;
import com.omnisolve.event.DocumentSubmittedEvent;
import com.omnisolve.event.DocumentUploadedEvent;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.repository.DocumentRepository;
import com.omnisolve.repository.DocumentStatusRepository;
import com.omnisolve.repository.DocumentTypeRepository;
import com.omnisolve.repository.DocumentVersionRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.security.AuthenticatedUser;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.dto.DocumentAttentionResponse;
import com.omnisolve.service.dto.DocumentRequest;
import com.omnisolve.service.dto.DocumentResponse;
import com.omnisolve.service.dto.DocumentStatsResponse;
import com.omnisolve.service.dto.DocumentVersionResponse;
import com.omnisolve.service.dto.DocumentWorkflowStatsResponse;
import com.omnisolve.service.dto.OverdueReviewItem;
import com.omnisolve.service.dto.PendingApprovalItem;
import com.omnisolve.service.dto.UpcomingReviewResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Business logic for the Document Control domain.
 *
 * <p>All write operations are:
 * <ul>
 *   <li>Wrapped in a database transaction via {@code @Transactional}.</li>
 *   <li>Annotated with {@code @Auditable} so the AOP layer writes an immutable
 *       audit record asynchronously.</li>
 *   <li>Followed by a domain event published via {@link ApplicationEventPublisher}
 *       to decouple side effects (notifications, timeline entries) from the
 *       core business logic.</li>
 * </ul>
 *
 * <p>Read operations use {@code @Transactional(readOnly = true)} to allow the
 * JDBC driver / Hibernate to optimise connection usage, and selected list
 * endpoints are cached via {@link CacheConfig} constants.
 *
 * <p>Tenant isolation is enforced through {@link SecurityContextFacade#currentUser()}
 * which resolves the {@code organisationId} for the authenticated user. Every
 * repository query that touches tenant-scoped data includes {@code organisationId}
 * as a filter parameter.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final String STATUS_DRAFT            = "Draft";
    private static final String STATUS_PENDING_APPROVAL = "Pending Approval";
    private static final String STATUS_ACTIVE           = "Active";
    private static final String STATUS_ARCHIVED         = "Archived";

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentStatusRepository documentStatusRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final OrganisationRepository organisationRepository;
    private final S3StorageService s3StorageService;
    private final SecurityContextFacade securityContextFacade;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentTypeRepository documentTypeRepository,
            DepartmentRepository departmentRepository,
            DocumentStatusRepository documentStatusRepository,
            DocumentVersionRepository documentVersionRepository,
            OrganisationRepository organisationRepository,
            S3StorageService s3StorageService,
            SecurityContextFacade securityContextFacade,
            ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.departmentRepository = departmentRepository;
        this.documentStatusRepository = documentStatusRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.organisationRepository = organisationRepository;
        this.s3StorageService = s3StorageService;
        this.securityContextFacade = securityContextFacade;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching documents: organisationId={}", organisationId);
        List<DocumentResponse> docs = documentRepository.findByOrganisationId(organisationId)
                .stream().map(this::toResponse).toList();
        log.info("Retrieved {} documents: organisationId={}", docs.size(), organisationId);
        return docs;
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching document: id={}, organisationId={}", id, organisationId);
        Document document = documentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> {
                    log.warn("Document not found or access denied: id={}, organisationId={}", id, organisationId);
                    return new ResponseStatusException(NOT_FOUND, "Document not found");
                });
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentStatsResponse getStats() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Calculating document stats: organisationId={}", organisationId);

        long total    = documentRepository.countByOrganisationId(organisationId);
        long active   = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_ACTIVE).getId());
        long pending  = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_PENDING_APPROVAL).getId());
        long draft    = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_DRAFT).getId());
        long archived = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_ARCHIVED).getId());
        long reviewDue = documentRepository.countReviewDueByOrganisation(organisationId, OffsetDateTime.now());

        return new DocumentStatsResponse(total, active, pending, reviewDue, draft, archived);
    }

    @Transactional(readOnly = true)
    public DocumentAttentionResponse getAttention() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        OffsetDateTime now = OffsetDateTime.now();

        List<PendingApprovalItem> pending = documentRepository
                .findPendingApprovalByOrganisation(organisationId).stream()
                .map(doc -> new PendingApprovalItem(
                        doc.getId(), doc.getDocumentNumber(), doc.getTitle(),
                        doc.getDepartment().getName(), doc.getOwnerId(), doc.getUpdatedAt()))
                .toList();

        List<OverdueReviewItem> overdue = documentRepository
                .findOverdueReviewsByOrganisation(organisationId, now).stream()
                .map(doc -> {
                    long days = ChronoUnit.DAYS.between(doc.getNextReviewAt(), now);
                    return new OverdueReviewItem(
                            doc.getId(), doc.getDocumentNumber(), doc.getTitle(),
                            doc.getDepartment().getName(), doc.getNextReviewAt(), days);
                })
                .toList();

        return new DocumentAttentionResponse(pending, overdue);
    }

    @Transactional(readOnly = true)
    public List<UpcomingReviewResponse> getUpcomingReviews(int days) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        OffsetDateTime now    = OffsetDateTime.now();
        OffsetDateTime future = now.plusDays(days);

        return documentRepository.findUpcomingReviewsByOrganisation(organisationId, now, future).stream()
                .map(doc -> {
                    long daysUntil = ChronoUnit.DAYS.between(now, doc.getNextReviewAt());
                    return new UpcomingReviewResponse(
                            doc.getId(), doc.getDocumentNumber(), doc.getTitle(),
                            doc.getDepartment().getName(), doc.getNextReviewAt(), daysUntil);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentWorkflowStatsResponse getWorkflowStats() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        long draft    = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_DRAFT).getId());
        long pending  = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_PENDING_APPROVAL).getId());
        long active   = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_ACTIVE).getId());
        long archived = documentRepository.countByOrganisationIdAndStatusId(organisationId, findStatus(STATUS_ARCHIVED).getId());
        return new DocumentWorkflowStatsResponse(draft, pending, active, archived);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getDocumentVersions(UUID documentId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        // Verify document belongs to this tenant before returning versions
        documentRepository.findByIdAndOrganisationId(documentId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(v -> new DocumentVersionResponse(
                        v.getId(), v.getDocument().getId(), v.getVersionNumber(),
                        v.getFileName(), v.getFileSize(), v.getMimeType(),
                        v.getS3Key(), v.getUploadedBy(), v.getUploadedAt()))
                .toList();
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    @Transactional
    @Auditable(action = "DOCUMENT_CREATED", entityType = "DOCUMENT")
    @CacheEvict(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentResponse create(DocumentRequest request, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        Long organisationId = user.organisationId();
        log.info("Creating document: title={}, typeId={}, organisationId={}", request.title(), request.typeId(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));

        DocumentType type = documentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));

        if (type.getRequiresClauses() && (request.clauseIds() == null || request.clauseIds().isEmpty())) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Document type '" + type.getName() + "' requires at least one clause to be linked");
        }

        String documentNumber = generateDocumentNumber(type, organisationId);
        log.info("Generated document number: {}", documentNumber);

        Document document = new Document();
        document.setOrganisation(organisation);
        document.setDocumentNumber(documentNumber);
        document.setTitle(request.title());
        document.setSummary(request.summary());
        document.setType(type);
        document.setDepartment(department);
        document.setOwnerId(request.ownerId());
        document.setStatus(findStatus(STATUS_DRAFT));
        document.setCreatedBy(request.createdBy() != null ? request.createdBy() : userId);
        document.setUpdatedBy(userId);
        document.setNextReviewAt(request.nextReviewAt());

        OffsetDateTime now = OffsetDateTime.now();
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        Document saved = documentRepository.save(document);
        log.info("Document created: id={}, documentNumber={}, organisationId={}", saved.getId(), saved.getDocumentNumber(), organisationId);
        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "DOCUMENT_UPDATED", entityType = "DOCUMENT")
    public DocumentResponse update(UUID id, DocumentRequest request, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Updating document: id={}, organisationId={}", id, organisationId);

        Document document = documentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        DocumentType type = documentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));

        if (type.getRequiresClauses() && (request.clauseIds() == null || request.clauseIds().isEmpty())) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Document type '" + type.getName() + "' requires at least one clause to be linked");
        }

        document.setTitle(request.title());
        document.setSummary(request.summary());
        document.setType(type);
        document.setDepartment(department);
        document.setOwnerId(request.ownerId());
        document.setNextReviewAt(request.nextReviewAt());
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());

        Document saved = documentRepository.save(document);
        log.info("Document updated: id={}, documentNumber={}", saved.getId(), saved.getDocumentNumber());
        return toResponse(saved);
    }

    @Transactional
    @Auditable(action = "DOCUMENT_SUBMITTED", entityType = "DOCUMENT")
    @CacheEvict(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentResponse submit(UUID id, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        DocumentResponse result = transition(id, STATUS_DRAFT, STATUS_PENDING_APPROVAL, userId, user.organisationId());
        eventPublisher.publishEvent(new DocumentSubmittedEvent(
                result.id(), result.documentNumber(), user.organisationId(), userId));
        return result;
    }

    @Transactional
    @Auditable(action = "DOCUMENT_APPROVED", entityType = "DOCUMENT")
    @CacheEvict(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentResponse approve(UUID id, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        DocumentResponse result = transition(id, STATUS_PENDING_APPROVAL, STATUS_ACTIVE, userId, user.organisationId());
        eventPublisher.publishEvent(new DocumentApprovedEvent(
                result.id(), result.documentNumber(), user.organisationId(), userId));
        return result;
    }

    @Transactional
    @Auditable(action = "DOCUMENT_REJECTED", entityType = "DOCUMENT")
    @CacheEvict(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentResponse reject(UUID id, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        DocumentResponse result = transition(id, STATUS_PENDING_APPROVAL, STATUS_DRAFT, userId, user.organisationId());
        eventPublisher.publishEvent(new DocumentRejectedEvent(
                result.id(), result.documentNumber(), user.organisationId(), userId));
        return result;
    }

    @Transactional
    @Auditable(action = "DOCUMENT_ARCHIVED", entityType = "DOCUMENT")
    @CacheEvict(value = CacheConfig.DOCUMENT_STATS, key = "#root.target.currentOrgId()")
    public DocumentResponse archive(UUID id, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        DocumentResponse result = transition(id, STATUS_ACTIVE, STATUS_ARCHIVED, userId, user.organisationId());
        eventPublisher.publishEvent(new DocumentArchivedEvent(
                result.id(), result.documentNumber(), user.organisationId(), userId));
        return result;
    }

    @Transactional
    @Auditable(action = "DOCUMENT_VERSION_UPLOADED", entityType = "DOCUMENT")
    public DocumentVersionResponse uploadVersion(UUID documentId, MultipartFile file, String userId) {
        AuthenticatedUser user = securityContextFacade.currentUser();
        log.info("Upload request: documentId={}, filename={}, size={}, user={}",
                documentId, file.getOriginalFilename(), file.getSize(), userId);

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }

        Document document = documentRepository.findByIdAndOrganisationId(documentId, user.organisationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        String statusName = document.getStatus().getName();
        if (!STATUS_DRAFT.equals(statusName) && !STATUS_ACTIVE.equals(statusName)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Cannot upload version. Document status must be Draft or Active. Current status: " + statusName);
        }

        if (file.getSize() > 50 * 1024 * 1024L) {
            throw new ResponseStatusException(BAD_REQUEST, "File size exceeds maximum allowed size of 50 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "File type not allowed. Received: " + (contentType != null ? contentType : "unknown"));
        }

        int nextVersion = documentVersionRepository
                .findTopByDocumentOrderByVersionNumberDesc(document)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        String safeFileName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String s3Key = "documents/" + documentId + "/v" + nextVersion + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, contentType);
            log.info("S3 upload success: documentId={}, s3Key={}", documentId, s3Key);
        } catch (IOException ex) {
            log.error("S3 upload failed: documentId={}, s3Key={}, error={}", documentId, s3Key, ex.getMessage(), ex);
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(nextVersion);
        version.setS3Key(s3Key);
        version.setFileName(safeFileName);
        version.setFileSize(file.getSize());
        version.setMimeType(contentType);
        version.setUploadedBy(userId);
        version.setUploadedAt(OffsetDateTime.now());

        DocumentVersion saved = documentVersionRepository.save(version);
        log.info("Document version saved: documentId={}, versionId={}, version={}", documentId, saved.getId(), nextVersion);

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                documentId, document.getDocumentNumber(), nextVersion, s3Key, safeFileName,
                user.organisationId(), userId));

        return new DocumentVersionResponse(
                saved.getId(), saved.getDocument().getId(), saved.getVersionNumber(),
                saved.getFileName(), saved.getFileSize(), saved.getMimeType(),
                saved.getS3Key(), saved.getUploadedBy(), saved.getUploadedAt());
    }

    // =========================================================================
    // Helpers used by @Cacheable SpEL expressions
    // =========================================================================

    /**
     * SpEL-accessible helper so that cache key expressions can reference the
     * current organisation without performing a full {@code currentUser()} DB call
     * when the context is already populated in {@link com.omnisolve.tenant.TenantContext}.
     */
    public Long currentOrgId() {
        Long fromContext = com.omnisolve.tenant.TenantContext.getOrganisationId();
        return fromContext != null ? fromContext : securityContextFacade.currentUser().organisationId();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private DocumentResponse transition(UUID id, String expectedCurrent, String target,
                                        String userId, Long organisationId) {
        log.info("Transitioning document: id={}, from={}, to={}", id, expectedCurrent, target);
        Document document = documentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        String currentStatus = document.getStatus().getName();
        if (!currentStatus.equals(expectedCurrent)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Invalid state transition from " + currentStatus + " to " + target);
        }

        document.setStatus(findStatus(target));
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        Document saved = documentRepository.save(document);
        log.info("Document transitioned: id={}, documentNumber={}, newStatus={}", saved.getId(), saved.getDocumentNumber(), target);
        return toResponse(saved);
    }

    private DocumentStatus findStatus(String name) {
        return documentStatusRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Status not found: " + name));
    }

    private String generateDocumentNumber(DocumentType type, Long organisationId) {
        String prefix = type.getName().length() >= 3
                ? type.getName().substring(0, 3).toUpperCase()
                : type.getName().toUpperCase();
        int year = OffsetDateTime.now().getYear();
        String pattern = prefix + "-" + year + "-%";
        List<Document> existing = documentRepository.findByOrganisationAndTypeAndPattern(organisationId, type, pattern);

        int nextSequence = 1;
        for (Document doc : existing) {
            String[] parts = doc.getDocumentNumber().split("-");
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

    private static boolean isAllowedMimeType(String contentType) {
        return switch (contentType) {
            case "application/pdf",
                 "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.ms-excel",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.ms-powerpoint",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                 "text/plain", "text/csv", "text/markdown",
                 "application/rtf",
                 "application/vnd.oasis.opendocument.text",
                 "application/vnd.oasis.opendocument.spreadsheet",
                 "application/vnd.oasis.opendocument.presentation",
                 "application/json", "application/xml",
                 "image/png", "image/jpeg", "image/gif" -> true;
            default -> false;
        };
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getDocumentNumber(),
                document.getTitle(),
                document.getSummary(),
                document.getType().getId(),
                document.getType().getName(),
                document.getType().getRequiresClauses(),
                document.getDepartment().getId(),
                document.getDepartment().getName(),
                document.getStatus().getId(),
                document.getStatus().getName(),
                document.getOwnerId(),
                document.getCreatedBy(),
                document.getNextReviewAt(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
