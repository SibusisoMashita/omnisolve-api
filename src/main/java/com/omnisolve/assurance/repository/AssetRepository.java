package com.omnisolve.assurance.repository;

import com.omnisolve.assurance.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByOrganisationIdOrderByNameAsc(Long organisationId);

    Optional<Asset> findByIdAndOrganisationId(UUID id, Long organisationId);

    boolean existsByAssetTypeId(Long assetTypeId);
}
