package com.omnisolve.contractor.repository;

import com.omnisolve.contractor.domain.ContractorWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractorWorkerRepository extends JpaRepository<ContractorWorker, UUID> {

    List<ContractorWorker> findByContractorId(UUID contractorId);

    Optional<ContractorWorker> findByIdAndContractorId(UUID id, UUID contractorId);

    long countByContractorId(UUID contractorId);
}
