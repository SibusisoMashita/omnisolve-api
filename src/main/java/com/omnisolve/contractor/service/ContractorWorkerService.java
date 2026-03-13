package com.omnisolve.contractor.service;

import com.omnisolve.contractor.domain.Contractor;
import com.omnisolve.contractor.domain.ContractorWorker;
import com.omnisolve.contractor.repository.ContractorRepository;
import com.omnisolve.contractor.repository.ContractorWorkerRepository;
import com.omnisolve.contractor.service.dto.ContractorWorkerRequest;
import com.omnisolve.contractor.service.dto.ContractorWorkerResponse;
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
public class ContractorWorkerService {

    private static final Logger log = LoggerFactory.getLogger(ContractorWorkerService.class);

    private final ContractorWorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final SecurityContextFacade securityContextFacade;

    public ContractorWorkerService(
            ContractorWorkerRepository workerRepository,
            ContractorRepository contractorRepository,
            SecurityContextFacade securityContextFacade) {
        this.workerRepository = workerRepository;
        this.contractorRepository = contractorRepository;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<ContractorWorkerResponse> listWorkers(UUID contractorId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        verifyContractorAccess(contractorId, organisationId);
        return workerRepository.findByContractorId(contractorId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ContractorWorkerResponse addWorker(UUID contractorId, ContractorWorkerRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Adding worker to contractor: contractorId={}", contractorId);

        Contractor contractor = findContractor(contractorId, organisationId);

        ContractorWorker worker = new ContractorWorker();
        worker.setContractor(contractor);
        worker.setFirstName(request.firstName());
        worker.setLastName(request.lastName());
        worker.setIdNumber(request.idNumber());
        worker.setPhone(request.phone());
        worker.setEmail(request.email());
        if (request.status() != null) worker.setStatus(request.status());
        worker.setCreatedAt(OffsetDateTime.now());

        return toResponse(workerRepository.save(worker));
    }

    @Transactional
    public ContractorWorkerResponse updateWorker(UUID workerId, ContractorWorkerRequest request) {
        log.info("Updating worker: workerId={}", workerId);

        ContractorWorker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Worker not found"));

        if (request.firstName() != null) worker.setFirstName(request.firstName());
        if (request.lastName() != null) worker.setLastName(request.lastName());
        if (request.idNumber() != null) worker.setIdNumber(request.idNumber());
        if (request.phone() != null) worker.setPhone(request.phone());
        if (request.email() != null) worker.setEmail(request.email());
        if (request.status() != null) worker.setStatus(request.status());

        return toResponse(workerRepository.save(worker));
    }

    @Transactional
    public void deleteWorker(UUID workerId) {
        log.info("Deleting worker: workerId={}", workerId);
        ContractorWorker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Worker not found"));
        workerRepository.delete(worker);
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

    private ContractorWorkerResponse toResponse(ContractorWorker w) {
        return new ContractorWorkerResponse(
                w.getId(), w.getContractor().getId(),
                w.getFirstName(), w.getLastName(),
                w.getIdNumber(), w.getPhone(), w.getEmail(),
                w.getStatus(), w.getCreatedAt());
    }
}
