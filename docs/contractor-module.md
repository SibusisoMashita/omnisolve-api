# Contractor Management Module

## Purpose

The Contractor Management module tracks external contractors, their workers, compliance documentation, and site access permissions. It ensures contractors meet regulatory requirements (insurance, COIDA, safety files) before accessing organizational sites.

## Key Responsibilities

- Manage contractor companies and their workers
- Track compliance documentation with expiry dates
- Monitor document expiry and compliance status
- Control site access permissions
- Provide compliance dashboard and alerts
- Store contractor documents in S3
- Audit all contractor-related changes

## Key Entities

### Contractor

Core entity representing an external contractor company:

```java
@Entity
@Table(name = "contractors")
public class Contractor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "registration_number")
    private String registrationNumber;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "status")
    private String status;  // ACTIVE, INACTIVE, SUSPENDED
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

### ContractorWorker

Individual workers employed by a contractor:

```java
@Entity
@Table(name = "contractor_workers")
public class ContractorWorker {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "id_number")
    private String idNumber;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "status")
    private String status;  // ACTIVE, INACTIVE
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
```

### ContractorDocument

Compliance documents uploaded for contractors:

```java
@Entity
@Table(name = "contractor_documents")
public class ContractorDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;
    
    @ManyToOne
    @JoinColumn(name = "document_type_id")
    private ContractorDocumentType documentType;
    
    @Column(name = "s3_key", nullable = false)
    private String s3Key;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type")
    private String mimeType;
    
    @Column(name = "issued_at")
    private LocalDate issuedAt;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;
}
```

### ContractorDocumentType

Reference data defining required compliance documents:

```java
@Entity
@Table(name = "contractor_document_types")
public class ContractorDocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "requires_expiry", nullable = false)
    private Boolean requiresExpiry;  // Does this document expire?
}
```

**Standard Document Types:**
- Insurance Certificate
- COIDA Letter (Compensation for Occupational Injuries and Diseases Act)
- Safety File
- Tax Clearance Certificate
- BEE Certificate (Broad-Based Black Economic Empowerment)

### ContractorSite

Many-to-many relationship for site access:

```java
@Entity
@Table(name = "contractor_sites")
public class ContractorSite {
    @EmbeddedId
    private ContractorSiteId id;
    
    @ManyToOne
    @MapsId("contractorId")
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;
    
    @ManyToOne
    @MapsId("siteId")
    @JoinColumn(name = "site_id")
    private Site site;
}

@Embeddable
public class ContractorSiteId implements Serializable {
    @Column(name = "contractor_id")
    private UUID contractorId;
    
    @Column(name = "site_id")
    private Long siteId;
}
```

## Compliance Tracking

### Compliance Status Calculation

Contractors are considered compliant when all required documents are:
1. Uploaded
2. Not expired (if expiry tracking is enabled)

```java
public record ContractorComplianceResponse(
    UUID contractorId,
    String name,
    long requiredDocuments,
    long validDocuments,
    boolean isCompliant,
    List<ExpiringDocumentResponse> expiringDocuments
) {
    public boolean isCompliant() {
        return requiredDocuments == validDocuments;
    }
}
```

### Expiry Monitoring

Documents with expiry dates are monitored for upcoming expiration:

```java
@Query("""
    SELECT d FROM ContractorDocument d
    WHERE d.contractor.organisation.id = :organisationId
    AND d.expiryDate IS NOT NULL
    AND d.expiryDate BETWEEN :now AND :futureDate
    ORDER BY d.expiryDate ASC
    """)
List<ContractorDocument> findExpiringDocuments(
    @Param("organisationId") Long organisationId,
    @Param("now") LocalDate now,
    @Param("futureDate") LocalDate futureDate
);
```

**Expiry Thresholds:**
- **Critical**: Expires within 30 days
- **Warning**: Expires within 60 days
- **Expired**: Past expiry date

### Compliance Dashboard

```java
public record ContractorComplianceDashboardResponse(
    long totalContractors,
    long compliantContractors,
    long nonCompliantContractors,
    long expiringDocuments,
    List<ContractorComplianceResponse> contractors
) {}
```

## S3 Storage Strategy

Contractor documents are stored in S3:

**S3 Key Pattern:**
```
contractors/{contractor-id}/{document-type-id}/{uuid}/{filename}
```

**Examples:**
```
contractors/a1b2c3d4-5678-90ab-cdef-1234567890ab/1/f9e8d7c6-5432-10ab-cdef-0987654321ba/insurance.pdf
contractors/a1b2c3d4-5678-90ab-cdef-1234567890ab/2/a2b3c4d5-6789-01ab-cdef-1234567890cd/coida.pdf
```

**Upload Flow:**
```java
@Transactional
public ContractorDocumentResponse uploadDocument(
    UUID contractorId,
    Long documentTypeId,
    MultipartFile file,
    LocalDate issuedAt,
    LocalDate expiryDate,
    String userId
) {
    // Validate file
    if (file.isEmpty()) {
        throw new ResponseStatusException(BAD_REQUEST, "File cannot be empty");
    }
    
    // Verify tenant access
    Contractor contractor = findContractor(contractorId, organisationId);
    ContractorDocumentType docType = findDocumentType(documentTypeId);
    
    // Validate expiry date if required
    if (docType.getRequiresExpiry() && expiryDate == null) {
        throw new ResponseStatusException(BAD_REQUEST, 
            "Expiry date required for " + docType.getName());
    }
    
    // Generate S3 key
    String s3Key = String.format("contractors/%s/%d/%s/%s",
        contractorId, documentTypeId, UUID.randomUUID(), file.getOriginalFilename());
    
    // Upload to S3
    s3StorageService.upload(file.getBytes(), s3Key, file.getContentType());
    
    // Save metadata
    ContractorDocument document = new ContractorDocument();
    document.setContractor(contractor);
    document.setDocumentType(docType);
    document.setS3Key(s3Key);
    document.setFileName(file.getOriginalFilename());
    document.setFileSize(file.getSize());
    document.setMimeType(file.getContentType());
    document.setIssuedAt(issuedAt);
    document.setExpiryDate(expiryDate);
    document.setUploadedBy(userId);
    document.setUploadedAt(OffsetDateTime.now());
    
    return toResponse(contractorDocumentRepository.save(document));
}
```

## Site Access Management

Contractors can be granted access to specific sites:

```java
@Transactional
public void grantSiteAccess(UUID contractorId, Long siteId) {
    Contractor contractor = findContractor(contractorId, organisationId);
    Site site = findSite(siteId, organisationId);
    
    ContractorSiteId id = new ContractorSiteId(contractorId, siteId);
    ContractorSite contractorSite = new ContractorSite();
    contractorSite.setId(id);
    contractorSite.setContractor(contractor);
    contractorSite.setSite(site);
    
    contractorSiteRepository.save(contractorSite);
}

@Transactional
public void revokeSiteAccess(UUID contractorId, Long siteId) {
    ContractorSiteId id = new ContractorSiteId(contractorId, siteId);
    contractorSiteRepository.deleteById(id);
}
```

**Query Contractors by Site:**
```java
@Query("""
    SELECT c FROM Contractor c
    JOIN c.sites cs
    WHERE c.organisation.id = :organisationId
    AND cs.site.id = :siteId
    """)
List<Contractor> findByOrganisationIdAndSiteId(
    @Param("organisationId") Long organisationId,
    @Param("siteId") Long siteId
);
```

## Worker Management

Track individual workers employed by contractors:

```java
@Transactional
public ContractorWorkerResponse addWorker(
    UUID contractorId,
    ContractorWorkerRequest request,
    String userId
) {
    Contractor contractor = findContractor(contractorId, organisationId);
    
    ContractorWorker worker = new ContractorWorker();
    worker.setContractor(contractor);
    worker.setFirstName(request.firstName());
    worker.setLastName(request.lastName());
    worker.setIdNumber(request.idNumber());
    worker.setPhone(request.phone());
    worker.setEmail(request.email());
    worker.setStatus("ACTIVE");
    worker.setCreatedAt(OffsetDateTime.now());
    
    return toResponse(contractorWorkerRepository.save(worker));
}
```

**List Workers by Contractor:**
```java
@Query("""
    SELECT w FROM ContractorWorker w
    WHERE w.contractor.id = :contractorId
    AND w.contractor.organisation.id = :organisationId
    ORDER BY w.lastName, w.firstName
    """)
List<ContractorWorker> findByContractorIdAndOrganisationId(
    @Param("contractorId") UUID contractorId,
    @Param("organisationId") Long organisationId
);
```

## API Endpoints

**Contractor CRUD:**
- `GET /api/contractors` - List all contractors
- `GET /api/contractors/{id}` - Get contractor details
- `POST /api/contractors` - Create contractor
- `PUT /api/contractors/{id}` - Update contractor
- `DELETE /api/contractors/{id}` - Delete contractor

**Compliance:**
- `GET /api/contractors/compliance` - Compliance dashboard
- `GET /api/contractors/{id}/compliance` - Contractor compliance status
- `GET /api/contractors/documents/expiring?days=30` - Expiring documents

**Documents:**
- `GET /api/contractors/{id}/documents` - List contractor documents
- `POST /api/contractors/{id}/documents` - Upload document
- `DELETE /api/contractors/documents/{documentId}` - Delete document
- `GET /api/contractors/documents/{documentId}/download` - Download document

**Workers:**
- `GET /api/contractors/{id}/workers` - List workers
- `POST /api/contractors/{id}/workers` - Add worker
- `PUT /api/contractors/workers/{workerId}` - Update worker
- `DELETE /api/contractors/workers/{workerId}` - Delete worker

**Site Access:**
- `GET /api/contractors/{id}/sites` - List accessible sites
- `POST /api/contractors/{id}/sites/{siteId}` - Grant site access
- `DELETE /api/contractors/{id}/sites/{siteId}` - Revoke site access

## Compliance Projection

Custom projection for efficient compliance queries:

```java
public interface ContractorComplianceProjection {
    String getContractorId();
    String getName();
    long getRequiredDocuments();
    long getValidDocuments();
}
```

**Native Query:**
```sql
SELECT 
    c.id as contractorId,
    c.name as name,
    COUNT(DISTINCT dt.id) as requiredDocuments,
    COUNT(DISTINCT CASE 
        WHEN cd.expiry_date IS NULL OR cd.expiry_date >= CURRENT_DATE 
        THEN cd.document_type_id 
    END) as validDocuments
FROM contractors c
CROSS JOIN contractor_document_types dt
LEFT JOIN contractor_documents cd 
    ON c.id = cd.contractor_id 
    AND dt.id = cd.document_type_id
WHERE c.organisation_id = :organisationId
GROUP BY c.id, c.name
```

## Audit Trail

All contractor operations are audited:

**Audited Actions:**
- `CONTRACTOR_CREATED` - Contractor added
- `CONTRACTOR_UPDATED` - Details updated
- `CONTRACTOR_DELETED` - Contractor removed
- `CONTRACTOR_DOCUMENT_UPLOADED` - Compliance document uploaded
- `CONTRACTOR_DOCUMENT_DELETED` - Document removed
- `CONTRACTOR_WORKER_ADDED` - Worker added
- `CONTRACTOR_WORKER_UPDATED` - Worker details updated
- `CONTRACTOR_SITE_ACCESS_GRANTED` - Site access granted
- `CONTRACTOR_SITE_ACCESS_REVOKED` - Site access revoked

**Example Audit Log:**
```json
{
  "entity_name": "CONTRACTOR",
  "entity_id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "action": "CONTRACTOR_DOCUMENT_UPLOADED",
  "details": {
    "contractor_name": "ABC Electrical",
    "document_type": "Insurance Certificate",
    "expiry_date": "2025-12-31",
    "file_name": "insurance.pdf"
  },
  "performed_by": "safety.manager@example.com",
  "performed_at": "2024-03-12T10:30:00Z",
  "organisation_id": 1
}
```

## Notifications and Alerts

**Expiry Notifications:**
- Email alerts 60 days before expiry
- Email alerts 30 days before expiry
- Email alerts on expiry day
- Dashboard warnings for expiring documents

**Compliance Alerts:**
- Alert when contractor becomes non-compliant
- Alert when required document is missing
- Alert when document expires

## Use Cases

**Use Case 1: Onboard New Contractor**
1. Create contractor record
2. Grant site access permissions
3. Upload required compliance documents
4. Add contractor workers
5. Verify compliance status

**Use Case 2: Monitor Compliance**
1. View compliance dashboard
2. Identify non-compliant contractors
3. Check expiring documents
4. Request document renewals
5. Update documents as received

**Use Case 3: Site Access Control**
1. Check contractor compliance status
2. Verify site access permissions
3. Grant/revoke access as needed
4. Track contractor workers on site

## Database Schema

```sql
-- Contractors (tenant-scoped)
CREATE TABLE contractors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id BIGINT NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100),
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (organisation_id, name)
);

-- Contractor Workers
CREATE TABLE contractor_workers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contractor_id UUID NOT NULL REFERENCES contractors(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    id_number VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Document Types (global reference)
CREATE TABLE contractor_document_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    requires_expiry BOOLEAN NOT NULL DEFAULT TRUE
);

-- Contractor Documents
CREATE TABLE contractor_documents (
    id BIGSERIAL PRIMARY KEY,
    contractor_id UUID NOT NULL REFERENCES contractors(id) ON DELETE CASCADE,
    document_type_id BIGINT REFERENCES contractor_document_types(id),
    s3_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    issued_at DATE,
    expiry_date DATE,
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Site Access (many-to-many)
CREATE TABLE contractor_sites (
    contractor_id UUID NOT NULL REFERENCES contractors(id) ON DELETE CASCADE,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    PRIMARY KEY (contractor_id, site_id)
);
```

## Indexes

```sql
CREATE INDEX idx_contractors_org ON contractors(organisation_id);
CREATE INDEX idx_contractor_workers_contractor ON contractor_workers(contractor_id);
CREATE INDEX idx_contractor_documents_contractor ON contractor_documents(contractor_id);
CREATE INDEX idx_contractor_documents_type ON contractor_documents(document_type_id);
CREATE INDEX idx_contractor_documents_expiry ON contractor_documents(expiry_date);
CREATE INDEX idx_contractor_sites_site ON contractor_sites(site_id);
```
