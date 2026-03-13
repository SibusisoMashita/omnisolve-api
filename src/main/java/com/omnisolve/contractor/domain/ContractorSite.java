package com.omnisolve.contractor.domain;

import com.omnisolve.domain.Site;
import jakarta.persistence.*;

@Entity
@Table(name = "contractor_sites")
public class ContractorSite {

    @EmbeddedId
    private ContractorSiteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contractorId")
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("siteId")
    @JoinColumn(name = "site_id")
    private Site site;

    public ContractorSite() {}

    public ContractorSite(Contractor contractor, Site site) {
        this.contractor = contractor;
        this.site = site;
        this.id = new ContractorSiteId(contractor.getId(), site.getId());
    }

    public ContractorSiteId getId() { return id; }
    public Contractor getContractor() { return contractor; }
    public Site getSite() { return site; }
}
