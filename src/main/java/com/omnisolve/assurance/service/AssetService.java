package com.omnisolve.assurance.service;

import com.omnisolve.assurance.domain.Asset;
import com.omnisolve.assurance.domain.AssetType;
import com.omnisolve.assurance.dto.AssetRequest;
import com.omnisolve.assurance.dto.AssetResponse;
import com.omnisolve.assurance.dto.AssetTypeRequest;
import com.omnisolve.assurance.dto.AssetTypeResponse;
import com.omnisolve.assurance.repository.AssetRepository;
import com.omnisolve.assurance.repository.AssetTypeRepository;
import com.omnisolve.domain.Department;
import com.omnisolve.domain.Organisation;
import com.omnisolve.domain.Site;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.repository.SiteRepository;
import com.omnisolve.security.SecurityContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final OrganisationRepository organisationRepository;
    private final SiteRepository siteRepository;
    private final DepartmentRepository departmentRepository;
    private final SecurityContextFacade securityContextFacade;

    public AssetService(
            AssetRepository assetRepository,
            AssetTypeRepository assetTypeRepository,
            OrganisationRepository organisationRepository,
            SiteRepository siteRepository,
            DepartmentRepository departmentRepository,
            SecurityContextFacade securityContextFacade) {
        this.assetRepository = assetRepository;
        this.assetTypeRepository = assetTypeRepository;
        this.organisationRepository = organisationRepository;
        this.siteRepository = siteRepository;
        this.departmentRepository = departmentRepository;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> listAssets() {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Listing assets: organisationId={}", organisationId);
        return assetRepository.findByOrganisationIdOrderByNameAsc(organisationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getAsset(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        return toResponse(findAsset(id, organisationId));
    }

    @Transactional
    public AssetResponse createAsset(AssetRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Creating asset: name={}, organisationId={}", request.name(), organisationId);

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organisation not found"));
        AssetType assetType = assetTypeRepository.findById(request.assetTypeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));

        Site site = null;
        if (request.siteId() != null) {
            site = siteRepository.findById(request.siteId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Site not found"));
        }
        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));
        }

        OffsetDateTime now = OffsetDateTime.now();
        Asset asset = new Asset();
        asset.setOrganisation(organisation);
        asset.setAssetType(assetType);
        asset.setName(request.name());
        asset.setAssetTag(request.assetTag());
        asset.setSerialNumber(request.serialNumber());
        asset.setSite(site);
        asset.setDepartment(department);
        asset.setStatus(request.status() != null ? request.status() : "Active");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);

        Asset saved = assetRepository.save(asset);
        log.info("Asset created: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public AssetResponse updateAsset(UUID id, AssetRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Asset asset = findAsset(id, organisationId);

        AssetType assetType = assetTypeRepository.findById(request.assetTypeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));
        Site site = null;
        if (request.siteId() != null) {
            site = siteRepository.findById(request.siteId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Site not found"));
        }
        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));
        }

        asset.setAssetType(assetType);
        asset.setName(request.name());
        asset.setAssetTag(request.assetTag());
        asset.setSerialNumber(request.serialNumber());
        asset.setSite(site);
        asset.setDepartment(department);
        asset.setStatus(request.status() != null ? request.status() : asset.getStatus());
        asset.setUpdatedAt(OffsetDateTime.now());

        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public void deleteAsset(UUID id) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        Asset asset = findAsset(id, organisationId);
        assetRepository.delete(asset);
        log.info("Asset deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<AssetTypeResponse> listAssetTypes() {
        return assetTypeRepository.findAll().stream()
                .map(at -> new AssetTypeResponse(at.getId(), at.getName(), at.getDescription()))
                .toList();
    }

    @Transactional
    public AssetTypeResponse createAssetType(AssetTypeRequest request) {
        AssetType assetType = new AssetType();
        assetType.setName(request.name());
        assetType.setDescription(request.description());
        AssetType saved = assetTypeRepository.save(assetType);
        log.info("AssetType created: id={}, name={}", saved.getId(), saved.getName());
        return new AssetTypeResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public AssetTypeResponse updateAssetType(Long id, AssetTypeRequest request) {
        AssetType assetType = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));
        assetType.setName(request.name());
        assetType.setDescription(request.description());
        AssetType saved = assetTypeRepository.save(assetType);
        return new AssetTypeResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public void deleteAssetType(Long id) {
        AssetType assetType = assetTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));
        if (assetRepository.existsByAssetTypeId(id)) {
            throw new ResponseStatusException(CONFLICT, "Cannot delete asset type with existing assets");
        }
        assetTypeRepository.delete(assetType);
        log.info("AssetType deleted: id={}", id);
    }

    private Asset findAsset(UUID id, Long organisationId) {
        return assetRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset not found"));
    }

    private AssetResponse toResponse(Asset a) {
        return new AssetResponse(
                a.getId(),
                a.getOrganisation().getId(),
                a.getAssetType().getId(),
                a.getAssetType().getName(),
                a.getName(),
                a.getAssetTag(),
                a.getSerialNumber(),
                a.getSite() != null ? a.getSite().getId() : null,
                a.getSite() != null ? a.getSite().getName() : null,
                a.getDepartment() != null ? a.getDepartment().getId() : null,
                a.getDepartment() != null ? a.getDepartment().getName() : null,
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
