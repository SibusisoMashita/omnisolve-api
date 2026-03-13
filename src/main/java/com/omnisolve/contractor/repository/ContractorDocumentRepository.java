package com.omnisolve.contractor.repository;

import com.omnisolve.contractor.domain.ContractorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractorDocumentRepository extends JpaRepository<ContractorDocument, Long> {

    List<ContractorDocument> findByContractorIdOrderByUploadedAtDesc(UUID contractorId);

    Optional<ContractorDocument> findByIdAndContractorId(Long id, UUID contractorId);
}
