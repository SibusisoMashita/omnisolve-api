package com.omnisolve.contractor.repository;

import com.omnisolve.contractor.domain.ContractorSite;
import com.omnisolve.contractor.domain.ContractorSiteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractorSiteRepository extends JpaRepository<ContractorSite, ContractorSiteId> {

    List<ContractorSite> findByContractorId(UUID contractorId);
}
