package com.omnisolve.assurance.controller;

import com.omnisolve.assurance.dto.AssetRequest;
import com.omnisolve.assurance.dto.AssetResponse;
import com.omnisolve.assurance.dto.AssetTypeRequest;
import com.omnisolve.assurance.dto.AssetTypeResponse;
import com.omnisolve.assurance.dto.InspectionResponse;
import com.omnisolve.assurance.service.AssetService;
import com.omnisolve.assurance.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Assets", description = "Manage organisational assets for inspection")
public class AssetController {

    private final AssetService assetService;
    private final InspectionService inspectionService;

    public AssetController(AssetService assetService, InspectionService inspectionService) {
        this.assetService = assetService;
        this.inspectionService = inspectionService;
    }

    @GetMapping("/api/assets")
    @Operation(summary = "List all assets for the organisation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assets retrieved successfully")
    })
    public List<AssetResponse> listAssets() {
        return assetService.listAssets();
    }

    @PostMapping("/api/assets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new asset")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public AssetResponse createAsset(@Valid @RequestBody AssetRequest request) {
        return assetService.createAsset(request);
    }

    @GetMapping("/api/assets/{id}")
    @Operation(summary = "Get asset by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public AssetResponse getAsset(
            @Parameter(description = "Asset UUID") @PathVariable UUID id) {
        return assetService.getAsset(id);
    }

    @PutMapping("/api/assets/{id}")
    @Operation(summary = "Update an asset")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset updated successfully"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public AssetResponse updateAsset(
            @Parameter(description = "Asset UUID") @PathVariable UUID id,
            @Valid @RequestBody AssetRequest request) {
        return assetService.updateAsset(id, request);
    }

    @DeleteMapping("/api/assets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an asset")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asset deleted"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public void deleteAsset(
            @Parameter(description = "Asset UUID") @PathVariable UUID id) {
        assetService.deleteAsset(id);
    }

    @GetMapping("/api/asset-types")
    @Operation(summary = "List all asset types")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset types retrieved successfully")
    })
    public List<AssetTypeResponse> listAssetTypes() {
        return assetService.listAssetTypes();
    }

    @PostMapping("/api/asset-types")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an asset type")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset type created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public AssetTypeResponse createAssetType(@Valid @RequestBody AssetTypeRequest request) {
        return assetService.createAssetType(request);
    }

    @PutMapping("/api/asset-types/{id}")
    @Operation(summary = "Update an asset type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset type updated"),
            @ApiResponse(responseCode = "404", description = "Asset type not found")
    })
    public AssetTypeResponse updateAssetType(
            @Parameter(description = "Asset type ID") @PathVariable Long id,
            @Valid @RequestBody AssetTypeRequest request) {
        return assetService.updateAssetType(id, request);
    }

    @DeleteMapping("/api/asset-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an asset type")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asset type deleted"),
            @ApiResponse(responseCode = "404", description = "Asset type not found"),
            @ApiResponse(responseCode = "409", description = "Asset type has existing assets")
    })
    public void deleteAssetType(
            @Parameter(description = "Asset type ID") @PathVariable Long id) {
        assetService.deleteAssetType(id);
    }

    @GetMapping("/api/assets/{assetId}/inspections")
    @Operation(summary = "Get inspection history for an asset")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public List<InspectionResponse> getAssetInspections(
            @Parameter(description = "Asset UUID") @PathVariable UUID assetId) {
        return inspectionService.getInspectionHistoryForAsset(assetId);
    }
}
