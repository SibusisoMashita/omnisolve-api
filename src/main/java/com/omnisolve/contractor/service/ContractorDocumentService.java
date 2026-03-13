package com.omnisolve.contractor.service;

import com.omnisolve.contractor.domain.Contractor;
import com.omnisolve.contractor.domain.ContractorDocument;
import com.omnisolve.contractor.domain.ContractorDocumentType;
import com.omnisolve.contractor.repository.ContractorDocumentRepository;
import com.omnisolve.contractor.repository.ContractorDocumentTypeRepository;
import com.omnisolve.contractor.repository.ContractorRepository;
import com.omnisolve.contractor.service.dto.ContractorDocumentRequest;
import com.omnisolve.contractor.service.dto.ContractorDocumentResponse;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ContractorDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ContractorDocumentService.class);

    private final ContractorDocumentRepository documentRepository;
    private final ContractorDocumentTypeRepository documentTypeRepository;
    private final ContractorRepository contractorRepository;
    private final S3StorageService s3StorageService;
    private final SecurityContextFacade securityContextFacade;

    public ContractorDocumentService(
            ContractorDocumentRepository documentRepository,
            ContractorDocumentTypeRepository documentTypeRepository,
            ContractorRepository contractorRepository,
            S3StorageService s3StorageService,
            SecurityContextFacade securityContextFacade) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.contractorRepository = contractorRepository;
        this.s3StorageService = s3StorageService;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<ContractorDocumentResponse> listDocuments(UUID contractorId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        verifyContractorAccess(contractorId, organisationId);
        return documentRepository.findByContractorIdOrderByUploadedAtDesc(contractorId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ContractorDocumentResponse uploadDocument(
            UUID contractorId, MultipartFile file, ContractorDocumentRequest request, String userId) {

        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Uploading document: contractorId={}, filename={}", contractorId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
        }

        Contractor contractor = findContractor(contractorId, organisationId);
        ContractorDocumentType docType = null;
        if (request.documentTypeId() != null) {
            docType = documentTypeRepository.findById(request.documentTypeId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
        }

        String safeFileName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        String s3Key = "contractors/" + contractorId + "/" + safeFileName;

        try {
            s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
        } catch (IOException ex) {
            log.error("S3 upload failed: contractorId={}, error={}", contractorId, ex.getMessage(), ex);
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        ContractorDocument doc = new ContractorDocument();
        doc.setContractor(contractor);
        doc.setDocumentType(docType);
        doc.setS3Key(s3Key);
        doc.setFileName(safeFileName);
        doc.setFileSize(file.getSize());
        doc.setMimeType(file.getContentType());
        doc.setIssuedAt(request.issuedAt());
        doc.setExpiryDate(request.expiryDate());
        doc.setUploadedBy(userId);
        doc.setUploadedAt(OffsetDateTime.now());

        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public ContractorDocumentResponse replaceDocument(
            UUID contractorId, Long documentId, MultipartFile file, ContractorDocumentRequest request, String userId) {

        Long organisationId = securityContextFacade.currentUser().organisationId();
        verifyContractorAccess(contractorId, organisationId);

        ContractorDocument doc = documentRepository.findByIdAndContractorId(documentId, contractorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        if (file != null && !file.isEmpty()) {
            String safeFileName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
            String s3Key = "contractors/" + contractorId + "/" + safeFileName;
            try {
                s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
            } catch (IOException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
            }
            doc.setS3Key(s3Key);
            doc.setFileName(safeFileName);
            doc.setFileSize(file.getSize());
            doc.setMimeType(file.getContentType());
        }

        if (request.documentTypeId() != null) {
            ContractorDocumentType docType = documentTypeRepository.findById(request.documentTypeId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document type not found"));
            doc.setDocumentType(docType);
        }
        if (request.issuedAt() != null) doc.setIssuedAt(request.issuedAt());
        if (request.expiryDate() != null) doc.setExpiryDate(request.expiryDate());
        doc.setUploadedBy(userId);
        doc.setUploadedAt(OffsetDateTime.now());

        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Deleting contractor document: documentId={}", documentId);

        ContractorDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        // Verify tenant ownership via contractor
        if (!contractorRepository.existsByIdAndOrganisationId(doc.getContractor().getId(), organisationId)) {
            throw new ResponseStatusException(NOT_FOUND, "Document not found");
        }

        documentRepository.delete(doc);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void verifyContractorAccess(UUID contractorId, Long organisationId) {
        if (!contractorRepository.existsByIdAndOrganisationId(contractorId, organisationId)) {
            throw new ResponseStatusException(NOT_FOUND, "Contractor not found");
        }
    }

    private Contractor findContractor(UUID contractorId, Long organisationId) {
        return contractorRepository.findByIdAndOrganisationId(contractorId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contractor not found"));
    }

    private ContractorDocumentResponse toResponse(ContractorDocument d) {
        LocalDate expiry = d.getExpiryDate();
        String docStatus = computeDocumentStatus(expiry);

        return new ContractorDocumentResponse(
                d.getId(),
                d.getContractor().getId(),
                d.getDocumentType() != null ? d.getDocumentType().getId() : null,
                d.getDocumentType() != null ? d.getDocumentType().getName() : null,
                d.getS3Key(),
                d.getFileName(),
                d.getFileSize(),
                d.getMimeType(),
                d.getIssuedAt(),
                d.getExpiryDate(),
                docStatus,
                d.getUploadedBy(),
                d.getUploadedAt());
    }

    private String computeDocumentStatus(LocalDate expiryDate) {
        if (expiryDate == null) return "VALID";
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) return "EXPIRED";
        if (expiryDate.isBefore(today.plusDays(30))) return "EXPIRING_SOON";
        return "VALID";
    }
}
