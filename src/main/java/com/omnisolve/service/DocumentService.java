package com.omnisolve.service;

import com.omnisolve.domain.Department;
import com.omnisolve.domain.Document;
import com.omnisolve.domain.DocumentStatus;
import com.omnisolve.domain.DocumentType;
import com.omnisolve.domain.DocumentVersion;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.repository.DocumentRepository;
import com.omnisolve.repository.DocumentStatusRepository;
import com.omnisolve.repository.DocumentTypeRepository;
import com.omnisolve.repository.DocumentVersionRepository;
import com.omnisolve.service.dto.DocumentRequest;
import com.omnisolve.service.dto.DocumentResponse;
import com.omnisolve.service.dto.DocumentStatsResponse;
import com.omnisolve.service.dto.DocumentVersionResponse;
import com.omnisolve.service.dto.DocumentAttentionResponse;
import com.omnisolve.service.dto.DocumentWorkflowStatsResponse;
import com.omnisolve.service.dto.OverdueReviewItem;
import com.omnisolve.service.dto.PendingApprovalItem;
import com.omnisolve.service.dto.UpcomingReviewResponse;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DocumentService {

    private static final String STATUS_DRAFT = "Draft";
    private static final String STATUS_PENDING_APPROVAL = "Pending Approval";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_ARCHIVED = "Archived";

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentStatusRepository documentStatusRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final S3StorageService s3StorageService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentTypeRepository documentTypeRepository,
            DepartmentRepository departmentRepository,
            DocumentStatusRepository documentStatusRepository,
            DocumentVersionRepository documentVersionRepository,
            S3StorageService s3StorageService
    ) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.departmentRepository = departmentRepository;
        this.documentStatusRepository = documentStatusRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id) {
        return toResponse(findDocument(id));
    }

    @Transactional(readOnly = true)
    public DocumentStatsResponse getStats() {
        long total = documentRepository.count();
        long active = documentRepository.countByStatus(findStatus(STATUS_ACTIVE));
        long pending = documentRepository.countByStatus(findStatus(STATUS_PENDING_APPROVAL));
        long draft = documentRepository.countByStatus(findStatus(STATUS_DRAFT));
        long archived = documentRepository.countByStatus(findStatus(STATUS_ARCHIVED));
        long reviewDue = documentRepository.countReviewDue(OffsetDateTime.now());

        return new DocumentStatsResponse(total, active, pending, reviewDue, draft, archived);
    }

    @Transactional
    public DocumentResponse create(DocumentRequest request, String userId) {
        DocumentType type = documentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));

        // Validate clauses requirement
        if (type.getRequiresClauses() && (request.clauseIds() == null || request.clauseIds().isEmpty())) {
            throw new ResponseStatusException(BAD_REQUEST, 
                    "Document type '" + type.getName() + "' requires at least one clause to be linked");
        }

        // Generate document number
        String documentNumber = generateDocumentNumber(type);

        Document document = new Document();
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

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse update(UUID id, DocumentRequest request, String userId) {
        Document document = findDocument(id);
        DocumentType type = documentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));

        // Validate clauses requirement
        if (type.getRequiresClauses() && (request.clauseIds() == null || request.clauseIds().isEmpty())) {
            throw new ResponseStatusException(BAD_REQUEST, 
                    "Document type '" + type.getName() + "' requires at least one clause to be linked");
        }

        // Note: documentNumber is not updated - it's auto-generated and immutable
        document.setTitle(request.title());
        document.setSummary(request.summary());
        document.setType(type);
        document.setDepartment(department);
        document.setOwnerId(request.ownerId());
        document.setNextReviewAt(request.nextReviewAt());
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse submit(UUID id, String userId) {
        return transition(id, STATUS_DRAFT, STATUS_PENDING_APPROVAL, userId);
    }

    @Transactional
    public DocumentResponse approve(UUID id, String userId) {
        return transition(id, STATUS_PENDING_APPROVAL, STATUS_ACTIVE, userId);
    }

    @Transactional
    public DocumentResponse reject(UUID id, String userId) {
        return transition(id, STATUS_PENDING_APPROVAL, STATUS_DRAFT, userId);
    }

    @Transactional
    public DocumentResponse archive(UUID id, String userId) {
        return transition(id, STATUS_ACTIVE, STATUS_ARCHIVED, userId);
    }

    @Transactional
    public DocumentVersionResponse uploadVersion(UUID documentId, MultipartFile file, String userId) {
        // Validate file is not empty
        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }

        // Validate document exists and get status
        Document document = findDocument(documentId);
        String statusName = document.getStatus().getName();
        
        // Validate document status allows upload
        if (!STATUS_DRAFT.equals(statusName) && !STATUS_ACTIVE.equals(statusName)) {
            throw new ResponseStatusException(BAD_REQUEST, 
                    "Cannot upload version. Document status must be Draft or Active. Current status: " + statusName);
        }

        // Validate file size (50 MB limit)
        long maxFileSize = 50 * 1024 * 1024; // 50 MB
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(BAD_REQUEST, 
                    "File size exceeds maximum allowed size of 50 MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        List<String> allowedMimeTypes = List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "text/csv",
                "text/markdown",
                "application/rtf",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.oasis.opendocument.presentation",
                "application/json",
                "application/xml",
                "image/png",
                "image/jpeg",
                "image/gif"
        );
        
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "File type not allowed. Supported common types include PDF, Word, Excel, PowerPoint, text, CSV, markdown, JSON, XML, and common images. Received: " +
                    (contentType != null ? contentType : "unknown"));
        }

        // Generate next version number
        int nextVersion = documentVersionRepository.findTopByDocumentOrderByVersionNumberDesc(document)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        String safeFileName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String s3Key = "documents/" + documentId + "/v" + nextVersion + "/" + safeFileName;

        // Upload to S3
        try {
            s3StorageService.upload(file.getBytes(), s3Key, contentType);
        } catch (IOException ioException) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ioException);
        }

        // Create version record
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
        return new DocumentVersionResponse(
                saved.getId(),
                saved.getDocument().getId(),
                saved.getVersionNumber(),
                saved.getFileName(),
                saved.getFileSize(),
                saved.getMimeType(),
                saved.getS3Key(),
                saved.getUploadedBy(),
                saved.getUploadedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getDocumentVersions(UUID documentId) {
        // Verify document exists
        findDocument(documentId);
        
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(version -> new DocumentVersionResponse(
                        version.getId(),
                        version.getDocument().getId(),
                        version.getVersionNumber(),
                        version.getFileName(),
                        version.getFileSize(),
                        version.getMimeType(),
                        version.getS3Key(),
                        version.getUploadedBy(),
                        version.getUploadedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentAttentionResponse getAttention() {
        OffsetDateTime now = OffsetDateTime.now();

        List<PendingApprovalItem> pending = documentRepository.findPendingApproval().stream()
                .map(doc -> new PendingApprovalItem(
                        doc.getId(),
                        doc.getDocumentNumber(),
                        doc.getTitle(),
                        doc.getDepartment().getName(),
                        doc.getOwnerId(),
                        doc.getUpdatedAt()
                ))
                .toList();

        List<OverdueReviewItem> overdue = documentRepository.findOverdueReviews(now).stream()
                .map(doc -> {
                    long daysOverdue = ChronoUnit.DAYS.between(doc.getNextReviewAt(), now);
                    return new OverdueReviewItem(
                            doc.getId(),
                            doc.getDocumentNumber(),
                            doc.getTitle(),
                            doc.getDepartment().getName(),
                            doc.getNextReviewAt(),
                            daysOverdue
                    );
                })
                .toList();

        return new DocumentAttentionResponse(pending, overdue);
    }

    @Transactional(readOnly = true)
    public List<UpcomingReviewResponse> getUpcomingReviews(int days) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime future = now.plusDays(days);

        return documentRepository.findUpcomingReviews(now, future).stream()
                .map(doc -> {
                    long daysUntil = ChronoUnit.DAYS.between(now, doc.getNextReviewAt());
                    return new UpcomingReviewResponse(
                            doc.getId(),
                            doc.getDocumentNumber(),
                            doc.getTitle(),
                            doc.getDepartment().getName(),
                            doc.getNextReviewAt(),
                            daysUntil
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentWorkflowStatsResponse getWorkflowStats() {
        long draft = documentRepository.countByStatus(findStatus(STATUS_DRAFT));
        long pending = documentRepository.countByStatus(findStatus(STATUS_PENDING_APPROVAL));
        long active = documentRepository.countByStatus(findStatus(STATUS_ACTIVE));
        long archived = documentRepository.countByStatus(findStatus(STATUS_ARCHIVED));

        return new DocumentWorkflowStatsResponse(draft, pending, active, archived);
    }

    private DocumentResponse transition(UUID id, String expectedCurrent, String target, String userId) {
        Document document = findDocument(id);
        if (!document.getStatus().getName().equals(expectedCurrent)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Invalid state transition from " + document.getStatus().getName() + " to " + target);
        }

        document.setStatus(findStatus(target));
        document.setUpdatedBy(userId);
        document.setUpdatedAt(OffsetDateTime.now());
        return toResponse(documentRepository.save(document));
    }

    private Document findDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
    }

    private DocumentStatus findStatus(String name) {
        return documentStatusRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Status not found: " + name));
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
                document.getUpdatedAt()
        );
    }

    private String generateDocumentNumber(DocumentType type) {
        // Get type prefix (first 3 letters uppercase)
        String prefix = type.getName().length() >= 3 
                ? type.getName().substring(0, 3).toUpperCase()
                : type.getName().toUpperCase();

        // Get current year
        int year = OffsetDateTime.now().getYear();

        // Find the latest document number for this type and year
        String pattern = prefix + "-" + year + "-%";
        List<Document> existingDocs = documentRepository.findByTypeAndPattern(type, pattern);
        
        int nextSequence = 1;
        if (!existingDocs.isEmpty()) {
            // Find the highest sequence number
            for (Document doc : existingDocs) {
                String docNumber = doc.getDocumentNumber();
                String[] parts = docNumber.split("-");
                if (parts.length == 3) {
                    try {
                        int seq = Integer.parseInt(parts[2]);
                        if (seq >= nextSequence) {
                            nextSequence = seq + 1;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid format
                    }
                }
            }
        }

        // Format: PREFIX-YEAR-SEQUENCE (e.g., POL-2026-001)
        return String.format("%s-%d-%03d", prefix, year, nextSequence);
    }
}

