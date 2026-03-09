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
import com.omnisolve.service.dto.DocumentVersionResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
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

    @Transactional
    public DocumentResponse create(DocumentRequest request, String userId) {
        DocumentType type = documentTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));

        Document document = new Document();
        document.setDocumentNumber(request.documentNumber());
        document.setTitle(request.title());
        document.setSummary(request.summary());
        document.setType(type);
        document.setDepartment(department);
        document.setOwnerId(request.ownerId());
        document.setStatus(findStatus(STATUS_DRAFT));
        document.setCreatedBy(userId);
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

        document.setDocumentNumber(request.documentNumber());
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
        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Uploaded file is empty");
        }

        Document document = findDocument(documentId);
        int nextVersion = documentVersionRepository.findTopByDocumentOrderByVersionNumberDesc(document)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        String safeFileName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String s3Key = "documents/" + documentId + "/v" + nextVersion + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        } catch (IOException ioException) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ioException);
        }

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(nextVersion);
        version.setS3Key(s3Key);
        version.setFileName(safeFileName);
        version.setFileSize(file.getSize());
        version.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        version.setUploadedBy(userId);
        version.setUploadedAt(OffsetDateTime.now());

        DocumentVersion saved = documentVersionRepository.save(version);
        return new DocumentVersionResponse(
                saved.getId(),
                saved.getVersionNumber(),
                saved.getFileName(),
                saved.getMimeType(),
                saved.getFileSize(),
                saved.getS3Key(),
                saved.getUploadedAt(),
                saved.getUploadedBy()
        );
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
                document.getStatus().getName(),
                document.getType().getName(),
                document.getDepartment().getName(),
                document.getOwnerId(),
                document.getNextReviewAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}

