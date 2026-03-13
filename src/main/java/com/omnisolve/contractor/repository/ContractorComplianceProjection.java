package com.omnisolve.contractor.repository;

public interface ContractorComplianceProjection {
    String getContractorId();
    String getContractorName();
    long getWorkers();
    long getRequiredDocuments();
    long getValidDocuments();
    long getExpiringDocuments();
    long getExpiredDocuments();
    long getCoveredDocuments();
}
