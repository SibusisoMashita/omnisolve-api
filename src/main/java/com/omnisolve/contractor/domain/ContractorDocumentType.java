package com.omnisolve.contractor.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "contractor_document_types")
public class ContractorDocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255, unique = true)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "requires_expiry", nullable = false)
    private boolean requiresExpiry = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequiresExpiry() { return requiresExpiry; }
    public void setRequiresExpiry(boolean requiresExpiry) { this.requiresExpiry = requiresExpiry; }
}
