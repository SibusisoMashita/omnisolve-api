package com.omnisolve.contractor.service;

import com.omnisolve.contractor.domain.Contractor;
import com.omnisolve.contractor.repository.ContractorComplianceProjection;
import com.omnisolve.contractor.repository.ContractorRepository;
import com.omnisolve.contractor.repository.ContractorWorkerRepository;
import com.omnisolve.contractor.service.dto.ContractorComplianceResponse;
import com.omnisolve.contractor.service.dto.ContractorRequest;
import com.omnisolve.contractor.service.dto.ContractorResponse;
import com.omnisolve.domain.Organisation;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.security.SecurityContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ContractorService {

    private static final Logger log = LoggerFactory.getLogger(ContractorService.class);

    private final ContractorRepository contractorRepository;
    private final ContractorWorkerRepository contractorWorkerRepository;
    private final OrganisationRepository organisationRepository;
    private final SecurityContextFacade securityContextFacade;

    public ContractorService(
            ContractorRepository contractorRepository,
            ContractorWorkerRepository contractorWorkerRepository,
            OrganisationRepository organisationRepository,
            SecurityContextFacade securityContextFacade) {
        this.contractorRepository = contractorRepository;
        this.contractorWorkerRepository = contractorWorkerRepository;
        this.organisationRepository = organisationRepository;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> listContractorsByOrganisation() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Listing contractors: organisationId={}", organisationId);
        return contractorRepository.findByOrganisationId(organisationId).stream()
                .map(c -> toResponse(c, contractorWorkerRepository.countByContractorId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContractorResponse getContractor(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Contractor c = findContractor(id, organisationId);
        return toResponse(c, contractorWorkerRepository.countByContractorId(c.getId()));
    }

    @Transactional
    public ContractorResponse createContractor(ContractorRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Creating contractor: name={}, organisationId={}", request.name(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));

        Contractor contractor = new Contractor();
        contractor.setOrganisation(organisation);
        contractor.setName(request.name());
        contractor.setRegistrationNumber(request.registrationNumber());
        contractor.setContactPerson(request.contactPerson());
        contractor.setEmail(request.email());
        contractor.setPhone(request.phone());
        if (request.status() != null) contractor.setStatus(request.status());

        OffsetDateTime now = OffsetDateTime.now();
        contractor.setCreatedAt(now);
        contractor.setUpdatedAt(now);

        Contractor saved = contractorRepository.save(contractor);
        log.info("Contractor created: id={}", saved.getId());
        return toResponse(saved, 0L);
    }

    @Transactional
    public ContractorResponse updateContractor(UUID id, ContractorRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Updating contractor: id={}", id);

        Contractor contractor = findContractor(id, organisationId);
        if (request.name() != null) contractor.setName(request.name());
        if (request.registrationNumber() != null) contractor.setRegistrationNumber(request.registrationNumber());
        if (request.contactPerson() != null) contractor.setContactPerson(request.contactPerson());
        if (request.email() != null) contractor.setEmail(request.email());
        if (request.phone() != null) contractor.setPhone(request.phone());
        if (request.status() != null) contractor.setStatus(request.status());
        contractor.setUpdatedAt(OffsetDateTime.now());

        Contractor saved = contractorRepository.save(contractor);
        return toResponse(saved, contractorWorkerRepository.countByContractorId(saved.getId()));
    }

    @Transactional
    public void deleteContractor(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Deleting contractor: id={}", id);
        Contractor contractor = findContractor(id, organisationId);
        contractorRepository.delete(contractor);
    }

    @Transactional(readOnly = true)
    public List<ContractorComplianceResponse> getComplianceByOrganisation() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Fetching compliance: organisationId={}", organisationId);
        return contractorRepository.findComplianceByOrganisationId(organisationId).stream()
                .map(this::toComplianceResponse)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private Contractor findContractor(UUID id, Long organisationId) {
        return contractorRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contractor not found"));
    }

    private ContractorResponse toResponse(Contractor c, long workerCount) {
        return new ContractorResponse(
                c.getId(), c.getName(), c.getRegistrationNumber(),
                c.getContactPerson(), c.getEmail(), c.getPhone(),
                c.getStatus(), workerCount, c.getCreatedAt(), c.getUpdatedAt());
    }

    private ContractorComplianceResponse toComplianceResponse(ContractorComplianceProjection p) {
        long required = p.getRequiredDocuments();
        long valid    = p.getValidDocuments();
        long expiring = p.getExpiringDocuments();
        long expired  = p.getExpiredDocuments();
        long missing  = p.getMissingDocuments();

        int score = required > 0 ? (int) Math.round((double) valid / required * 100) : 100;
        String status = computeStatus(required, valid, expired, expiring, missing);

        return new ContractorComplianceResponse(
                UUID.fromString(p.getContractorId()),
                p.getName(),
                required, valid, expiring, expired, missing,
                score, status);
    }

    private String computeStatus(long required, long valid, long expired, long expiring, long missing) {
        if (required == 0 || valid == required) return "COMPLIANT";
        if (expired > 0) return "NON_COMPLIANT";
        if (missing > 0) return "INCOMPLETE";
        if (expiring > 0) return "EXPIRING";
        return "COMPLIANT";
    }
}
