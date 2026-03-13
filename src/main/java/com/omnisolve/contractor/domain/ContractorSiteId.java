package com.omnisolve.contractor.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ContractorSiteId implements Serializable {

    private UUID contractorId;
    private Long siteId;

    public ContractorSiteId() {}

    public ContractorSiteId(UUID contractorId, Long siteId) {
        this.contractorId = contractorId;
        this.siteId = siteId;
    }

    public UUID getContractorId() { return contractorId; }
    public void setContractorId(UUID contractorId) { this.contractorId = contractorId; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractorSiteId other)) return false;
        return Objects.equals(contractorId, other.contractorId) && Objects.equals(siteId, other.siteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractorId, siteId);
    }
}
