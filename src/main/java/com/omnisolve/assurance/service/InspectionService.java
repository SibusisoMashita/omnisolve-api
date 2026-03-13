package com.omnisolve.assurance.service;

import com.omnisolve.assurance.domain.Asset;
import com.omnisolve.assurance.domain.Inspection;
import com.omnisolve.assurance.domain.InspectionAttachment;
import com.omnisolve.assurance.domain.InspectionChecklistItem;
import com.omnisolve.assurance.domain.InspectionFinding;
import com.omnisolve.assurance.domain.InspectionItem;
import com.omnisolve.assurance.domain.InspectionSeverity;
import com.omnisolve.assurance.domain.InspectionType;
import com.omnisolve.assurance.dto.ChecklistItemResponse;
import com.omnisolve.assurance.dto.ChecklistTemplateResponse;
import com.omnisolve.assurance.dto.InspectionAttachmentResponse;
import com.omnisolve.assurance.dto.InspectionDashboardResponse;
import com.omnisolve.assurance.dto.InspectionDetailResponse;
import com.omnisolve.assurance.dto.InspectionFindingRequest;
import com.omnisolve.assurance.dto.InspectionFindingResponse;
import com.omnisolve.assurance.dto.InspectionItemRequest;
import com.omnisolve.assurance.dto.InspectionItemResponse;
import com.omnisolve.assurance.dto.InspectionRequest;
import com.omnisolve.assurance.dto.InspectionResponse;
import com.omnisolve.assurance.repository.AssetRepository;
import com.omnisolve.assurance.repository.InspectionAttachmentRepository;
import com.omnisolve.assurance.repository.InspectionChecklistItemRepository;
import com.omnisolve.assurance.repository.InspectionChecklistRepository;
import com.omnisolve.assurance.repository.InspectionFindingRepository;
import com.omnisolve.assurance.repository.InspectionItemRepository;
import com.omnisolve.assurance.repository.InspectionRepository;
import com.omnisolve.assurance.repository.InspectionSeverityRepository;
import com.omnisolve.assurance.repository.InspectionTypeRepository;
import com.omnisolve.domain.Clause;
import com.omnisolve.domain.Organisation;
import com.omnisolve.repository.ClauseRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    private final InspectionRepository inspectionRepository;
    private final InspectionItemRepository inspectionItemRepository;
    private final InspectionFindingRepository inspectionFindingRepository;
    private final InspectionAttachmentRepository inspectionAttachmentRepository;
    private final InspectionChecklistRepository inspectionChecklistRepository;
    private final InspectionChecklistItemRepository inspectionChecklistItemRepository;
    private final AssetRepository assetRepository;
    private final OrganisationRepository organisationRepository;
    private final ClauseRepository clauseRepository;
    private final InspectionTypeRepository inspectionTypeRepository;
    private final InspectionSeverityRepository inspectionSeverityRepository;
    private final S3StorageService s3StorageService;
    private final SecurityContextFacade securityContextFacade;

    public InspectionService(
            InspectionRepository inspectionRepository,
            InspectionItemRepository inspectionItemRepository,
            InspectionFindingRepository inspectionFindingRepository,
            InspectionAttachmentRepository inspectionAttachmentRepository,
            InspectionChecklistRepository inspectionChecklistRepository,
            InspectionChecklistItemRepository inspectionChecklistItemRepository,
            AssetRepository assetRepository,
            OrganisationRepository organisationRepository,
            ClauseRepository clauseRepository,
            InspectionTypeRepository inspectionTypeRepository,
            InspectionSeverityRepository inspectionSeverityRepository,
            S3StorageService s3StorageService,
            SecurityContextFacade securityContextFacade) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.inspectionFindingRepository = inspectionFindingRepository;
        this.inspectionAttachmentRepository = inspectionAttachmentRepository;
        this.inspectionChecklistRepository = inspectionChecklistRepository;
        this.inspectionChecklistItemRepository = inspectionChecklistItemRepository;
        this.assetRepository = assetRepository;
        this.organisationRepository = organisationRepository;
        this.clauseRepository = clauseRepository;
        this.inspectionTypeRepository = inspectionTypeRepository;
        this.inspectionSeverityRepository = inspectionSeverityRepository;
        this.s3StorageService = s3StorageService;
        this.securityContextFacade = securityContextFacade;
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<InspectionResponse> listInspections(String status, String search, Pageable pageable) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Listing inspections: organisationId={}", organisationId);
        return inspectionRepository
                .findByOrganisationIdWithFilters(organisationId, status, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InspectionDetailResponse getInspection(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Inspection inspection = findInspection(id, organisationId);

        List<InspectionItemResponse> items = inspectionItemRepository
                .findByInspectionIdOrderById(id).stream().map(this::toItemResponse).toList();
        List<InspectionFindingResponse> findings = inspectionFindingRepository
                .findByInspectionIdOrderByCreatedAtDesc(id).stream().map(this::toFindingResponse).toList();
        List<InspectionAttachmentResponse> attachments = inspectionAttachmentRepository
                .findByInspectionIdOrderByUploadedAtDesc(id).stream().map(this::toAttachmentResponse).toList();

        return toDetailResponse(inspection, items, findings, attachments);
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> getInspectionHistoryForAsset(UUID assetId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        assetRepository.findByIdAndOrganisationId(assetId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset not found"));
        return inspectionRepository
                .findByAssetIdAndOrganisationIdOrderByCreatedAtDesc(assetId, organisationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InspectionDashboardResponse getDashboard() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        long total = inspectionRepository.countByOrganisationId(organisationId);
        long scheduled = inspectionRepository.countByOrganisationIdAndStatus(organisationId, "SCHEDULED");
        long inProgress = inspectionRepository.countByOrganisationIdAndStatus(organisationId, "IN_PROGRESS");
        long completed = inspectionRepository.countByOrganisationIdAndStatus(organisationId, "COMPLETED");
        long overdue = inspectionRepository.countOverdueByOrganisationId(organisationId);
        return new InspectionDashboardResponse(total, scheduled, inProgress, completed, overdue);
    }

    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> listChecklistTemplates() {
        return inspectionChecklistRepository.findAll().stream()
                .map(cl -> {
                    List<ChecklistItemResponse> items = inspectionChecklistItemRepository
                            .findByChecklistIdOrderBySortOrder(cl.getId()).stream()
                            .map(i -> new ChecklistItemResponse(i.getId(), i.getTitle(), i.getDescription(), i.getSortOrder()))
                            .toList();
                    String assetTypeName = cl.getAssetType() != null ? cl.getAssetType().getName() : null;
                    Long assetTypeId = cl.getAssetType() != null ? cl.getAssetType().getId() : null;
                    return new ChecklistTemplateResponse(cl.getId(), cl.getName(), cl.getDescription(), assetTypeId, assetTypeName, items);
                })
                .toList();
    }

    // =========================================================================
    // Write
    // =========================================================================

    @Transactional
    public InspectionResponse createInspection(InspectionRequest request, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Creating inspection: assetId={}, organisationId={}", request.assetId(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));
        Asset asset = assetRepository.findByIdAndOrganisationId(request.assetId(), organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset not found"));

        OffsetDateTime now = OffsetDateTime.now();
        String inspectionNumber = generateInspectionNumber(organisationId);

        InspectionType inspectionType = null;
        if (request.inspectionTypeId() != null) {
            inspectionType = inspectionTypeRepository.findById(request.inspectionTypeId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inspection type not found"));
        }

        Inspection inspection = new Inspection();
        inspection.setOrganisation(organisation);
        inspection.setAsset(asset);
        inspection.setInspectionType(inspectionType);
        inspection.setInspectionNumber(inspectionNumber);
        inspection.setTitle(request.title());
        inspection.setInspectorId(request.inspectorId() != null ? request.inspectorId() : userId);
        inspection.setStatus("SCHEDULED");
        inspection.setScheduledAt(request.scheduledAt());
        inspection.setCreatedAt(now);
        inspection.setUpdatedAt(now);

        Inspection saved = inspectionRepository.save(inspection);

        // Pre-populate items from checklist template if provided
        if (request.checklistId() != null) {
            List<InspectionChecklistItem> templateItems = inspectionChecklistItemRepository
                    .findByChecklistIdOrderBySortOrder(request.checklistId());
            for (InspectionChecklistItem templateItem : templateItems) {
                InspectionItem item = new InspectionItem();
                item.setInspection(saved);
                item.setChecklistItem(templateItem);
                item.setStatus("NOT_APPLICABLE");
                inspectionItemRepository.save(item);
            }
        }

        log.info("Inspection created: id={}, inspectionNumber={}", saved.getId(), saved.getInspectionNumber());
        return toResponse(saved);
    }

    @Transactional
    public InspectionResponse startInspection(UUID id, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Inspection inspection = findInspection(id, organisationId);

        if (!"SCHEDULED".equals(inspection.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Inspection must be in SCHEDULED status to start");
        }

        OffsetDateTime now = OffsetDateTime.now();
        inspection.setStatus("IN_PROGRESS");
        inspection.setStartedAt(now);
        inspection.setUpdatedAt(now);

        log.info("Inspection started: id={}", id);
        return toResponse(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse submitInspection(UUID id, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Inspection inspection = findInspection(id, organisationId);

        if (!"IN_PROGRESS".equals(inspection.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Inspection must be IN_PROGRESS to complete");
        }

        OffsetDateTime now = OffsetDateTime.now();
        inspection.setStatus("COMPLETED");
        inspection.setCompletedAt(now);
        inspection.setUpdatedAt(now);

        log.info("Inspection completed: id={}", id);
        return toResponse(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionItemResponse addInspectionItem(UUID inspectionId, InspectionItemRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Inspection inspection = findInspection(inspectionId, organisationId);

        InspectionChecklistItem checklistItem = null;
        if (request.checklistItemId() != null) {
            checklistItem = inspectionChecklistItemRepository.findById(request.checklistItemId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Checklist item not found"));
        }

        InspectionItem item = new InspectionItem();
        item.setInspection(inspection);
        item.setChecklistItem(checklistItem);
        item.setStatus(request.status());
        item.setNotes(request.notes());

        return toItemResponse(inspectionItemRepository.save(item));
    }

    @Transactional
    public InspectionFindingResponse addFinding(UUID inspectionId, InspectionFindingRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Inspection inspection = findInspection(inspectionId, organisationId);

        Clause clause = null;
        if (request.clauseId() != null) {
            clause = clauseRepository.findById(request.clauseId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clause not found"));
        }

        InspectionSeverity severity = inspectionSeverityRepository.findById(request.severityId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Severity not found"));

        InspectionFinding finding = new InspectionFinding();
        finding.setInspection(inspection);
        finding.setClause(clause);
        finding.setSeverity(severity);
        finding.setDescription(request.description());
        finding.setActionRequired(request.actionRequired());
        finding.setCreatedAt(OffsetDateTime.now());

        return toFindingResponse(inspectionFindingRepository.save(finding));
    }

    @Transactional
    public InspectionAttachmentResponse uploadInspectionAttachment(UUID inspectionId, MultipartFile file, String userId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Upload attachment: inspectionId={}, filename={}", inspectionId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }
        if (file.getSize() > 50 * 1024 * 1024L) {
            throw new ResponseStatusException(BAD_REQUEST, "File size exceeds maximum allowed size of 50 MB");
        }

        Inspection inspection = findInspection(inspectionId, organisationId);

        String safeFileName = file.getOriginalFilename() == null ? "attachment.bin" : file.getOriginalFilename();
        String s3Key = "inspections/" + inspection.getAsset().getId() + "/" + inspectionId + "/" + UUID.randomUUID() + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
        } catch (IOException ex) {
            log.error("S3 upload failed: inspectionId={}, error={}", inspectionId, ex.getMessage(), ex);
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        InspectionAttachment attachment = new InspectionAttachment();
        attachment.setInspection(inspection);
        attachment.setS3Key(s3Key);
        attachment.setFileName(safeFileName);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploadedBy(userId);
        attachment.setUploadedAt(OffsetDateTime.now());

        return toAttachmentResponse(inspectionAttachmentRepository.save(attachment));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Inspection findInspection(UUID id, Long organisationId) {
        return inspectionRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inspection not found"));
    }

    private String generateInspectionNumber(Long organisationId) {
        long count = inspectionRepository.countByOrganisationId(organisationId);
        int year = OffsetDateTime.now().getYear();
        return String.format("INS-%d-%04d", year, count + 1);
    }

    private InspectionResponse toResponse(Inspection i) {
        Long inspectionTypeId = i.getInspectionType() != null ? i.getInspectionType().getId() : null;
        String inspectionTypeName = i.getInspectionType() != null ? i.getInspectionType().getName() : null;
        return new InspectionResponse(
                i.getId(),
                i.getOrganisation().getId(),
                i.getAsset().getId(),
                i.getAsset().getName(),
                inspectionTypeId,
                inspectionTypeName,
                i.getInspectionNumber(),
                i.getTitle(),
                i.getInspectorId(),
                i.getStatus(),
                i.getScheduledAt(),
                i.getStartedAt(),
                i.getCompletedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt());
    }

    private InspectionDetailResponse toDetailResponse(Inspection i,
            List<InspectionItemResponse> items,
            List<InspectionFindingResponse> findings,
            List<InspectionAttachmentResponse> attachments) {
        Long inspectionTypeId = i.getInspectionType() != null ? i.getInspectionType().getId() : null;
        String inspectionTypeName = i.getInspectionType() != null ? i.getInspectionType().getName() : null;
        return new InspectionDetailResponse(
                i.getId(),
                i.getOrganisation().getId(),
                i.getAsset().getId(),
                i.getAsset().getName(),
                inspectionTypeId,
                inspectionTypeName,
                i.getInspectionNumber(),
                i.getTitle(),
                i.getInspectorId(),
                i.getStatus(),
                i.getScheduledAt(),
                i.getStartedAt(),
                i.getCompletedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                items,
                findings,
                attachments);
    }

    private InspectionItemResponse toItemResponse(InspectionItem item) {
        String checklistItemTitle = item.getChecklistItem() != null ? item.getChecklistItem().getTitle() : null;
        Long checklistItemId = item.getChecklistItem() != null ? item.getChecklistItem().getId() : null;
        return new InspectionItemResponse(
                item.getId(),
                item.getInspection().getId(),
                checklistItemId,
                checklistItemTitle,
                item.getStatus(),
                item.getNotes());
    }

    private InspectionFindingResponse toFindingResponse(InspectionFinding f) {
        String clauseCode = f.getClause() != null ? f.getClause().getCode() : null;
        Long clauseId = f.getClause() != null ? f.getClause().getId() : null;
        Long severityId = f.getSeverity() != null ? f.getSeverity().getId() : null;
        String severityName = f.getSeverity() != null ? f.getSeverity().getName() : null;
        return new InspectionFindingResponse(
                f.getId(),
                f.getInspection().getId(),
                clauseId,
                clauseCode,
                severityId,
                severityName,
                f.getDescription(),
                f.getActionRequired(),
                f.getCreatedAt());
    }

    private InspectionAttachmentResponse toAttachmentResponse(InspectionAttachment a) {
        return new InspectionAttachmentResponse(
                a.getId(),
                a.getInspection().getId(),
                a.getS3Key(),
                a.getFileName(),
                a.getFileSize(),
                a.getMimeType(),
                a.getUploadedBy(),
                a.getUploadedAt());
    }
}
