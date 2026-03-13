package com.omnisolve.assurance.controller;

import com.omnisolve.assurance.dto.ChecklistItemRequest;
import com.omnisolve.assurance.dto.ChecklistItemResponse;
import com.omnisolve.assurance.dto.ChecklistRequest;
import com.omnisolve.assurance.dto.ChecklistTemplateResponse;
import com.omnisolve.assurance.service.InspectionChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assurance/checklists")
@Tag(name = "Inspection Checklists", description = "Manage reusable inspection checklist templates")
public class InspectionChecklistController {

    private final InspectionChecklistService checklistService;

    public InspectionChecklistController(InspectionChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    @Operation(summary = "List all checklist templates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checklists retrieved successfully")
    })
    public List<ChecklistTemplateResponse> listChecklists() {
        return checklistService.listChecklists();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a checklist template by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checklist retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Checklist not found")
    })
    public ChecklistTemplateResponse getChecklist(
            @Parameter(description = "Checklist ID") @PathVariable Long id) {
        return checklistService.getChecklist(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new checklist template")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Checklist created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ChecklistTemplateResponse createChecklist(@Valid @RequestBody ChecklistRequest request) {
        return checklistService.createChecklist(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a checklist template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checklist updated"),
            @ApiResponse(responseCode = "404", description = "Checklist not found")
    })
    public ChecklistTemplateResponse updateChecklist(
            @Parameter(description = "Checklist ID") @PathVariable Long id,
            @Valid @RequestBody ChecklistRequest request) {
        return checklistService.updateChecklist(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a checklist template and all its items")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Checklist deleted"),
            @ApiResponse(responseCode = "404", description = "Checklist not found")
    })
    public void deleteChecklist(
            @Parameter(description = "Checklist ID") @PathVariable Long id) {
        checklistService.deleteChecklist(id);
    }

    @PostMapping("/{checklistId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an item to a checklist template")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added"),
            @ApiResponse(responseCode = "404", description = "Checklist not found")
    })
    public ChecklistItemResponse addChecklistItem(
            @Parameter(description = "Checklist ID") @PathVariable Long checklistId,
            @Valid @RequestBody ChecklistItemRequest request) {
        return checklistService.addChecklistItem(checklistId, request);
    }

    @PutMapping("/{checklistId}/items/{itemId}")
    @Operation(summary = "Update a checklist item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated"),
            @ApiResponse(responseCode = "404", description = "Checklist or item not found")
    })
    public ChecklistItemResponse updateChecklistItem(
            @Parameter(description = "Checklist ID") @PathVariable Long checklistId,
            @Parameter(description = "Item ID") @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemRequest request) {
        return checklistService.updateChecklistItem(checklistId, itemId, request);
    }

    @DeleteMapping("/{checklistId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a checklist item")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item deleted"),
            @ApiResponse(responseCode = "404", description = "Checklist or item not found")
    })
    public void deleteChecklistItem(
            @Parameter(description = "Checklist ID") @PathVariable Long checklistId,
            @Parameter(description = "Item ID") @PathVariable Long itemId) {
        checklistService.deleteChecklistItem(checklistId, itemId);
    }
}
