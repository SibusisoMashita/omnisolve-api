package com.omnisolve.repository;

import com.omnisolve.domain.Site;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByOrganisationId(Long organisationId);
}
