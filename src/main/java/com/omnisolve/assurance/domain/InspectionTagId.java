package com.omnisolve.assurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class InspectionTagId implements Serializable {

    @Column(name = "inspection_id")
    private UUID inspectionId;

    @Column(name = "tag_id")
    private Long tagId;

    public InspectionTagId() {}

    public InspectionTagId(UUID inspectionId, Long tagId) {
        this.inspectionId = inspectionId;
        this.tagId = tagId;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(UUID inspectionId) {
        this.inspectionId = inspectionId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InspectionTagId)) return false;
        InspectionTagId that = (InspectionTagId) o;
        return Objects.equals(inspectionId, that.inspectionId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inspectionId, tagId);
    }
}
