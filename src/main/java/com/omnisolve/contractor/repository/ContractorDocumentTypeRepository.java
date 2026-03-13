package com.omnisolve.contractor.repository;

import com.omnisolve.contractor.domain.ContractorDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractorDocumentTypeRepository extends JpaRepository<ContractorDocumentType, Long> {

    List<ContractorDocumentType> findAllByOrderByNameAsc();
}
