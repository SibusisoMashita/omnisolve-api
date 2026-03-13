package com.omnisolve.risk.domain;

import com.omnisolve.domain.Organisation;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "risks")
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private RiskCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "severity_id")
    private RiskSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "likelihood_id")
    private RiskLikelihood likelihood;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "owner_id", length = 255)
    private String ownerId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "OPEN";

    @Column(name = "identified_at", nullable = false)
    private OffsetDateTime identifiedAt;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RiskCategory getCategory() {
        return category;
    }

    public void setCategory(RiskCategory category) {
        this.category = category;
    }

    public RiskSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RiskSeverity severity) {
        this.severity = severity;
    }

    public RiskLikelihood getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(RiskLikelihood likelihood) {
        this.likelihood = likelihood;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getIdentifiedAt() {
        return identifiedAt;
    }

    public void setIdentifiedAt(OffsetDateTime identifiedAt) {
        this.identifiedAt = identifiedAt;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
