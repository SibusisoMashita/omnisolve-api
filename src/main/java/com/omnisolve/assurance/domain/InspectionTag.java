package com.omnisolve.assurance.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "inspection_tags")
public class InspectionTag {

    @EmbeddedId
    private InspectionTagId id = new InspectionTagId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("inspectionId")
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public InspectionTagId getId() {
        return id;
    }

    public Inspection getInspection() {
        return inspection;
    }

    public void setInspection(Inspection inspection) {
        this.inspection = inspection;
        this.id.setInspectionId(inspection.getId());
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
        this.id.setTagId(tag.getId());
    }
}
