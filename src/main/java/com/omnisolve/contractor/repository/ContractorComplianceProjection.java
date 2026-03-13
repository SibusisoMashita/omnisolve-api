package com.omnisolve.contractor.repository;

public interface ContractorComplianceProjection {
    String getContractorId();
    String getName();
    long getRequiredDocuments();
    long getValidDocuments();
    long getExpiringDocuments();
    long getExpiredDocuments();
    long getMissingDocuments();
}
