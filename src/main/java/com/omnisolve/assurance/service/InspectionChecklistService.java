package com.omnisolve.assurance.service;

import com.omnisolve.assurance.domain.AssetType;
import com.omnisolve.assurance.domain.InspectionChecklist;
import com.omnisolve.assurance.domain.InspectionChecklistItem;
import com.omnisolve.assurance.dto.ChecklistItemRequest;
import com.omnisolve.assurance.dto.ChecklistItemResponse;
import com.omnisolve.assurance.dto.ChecklistRequest;
import com.omnisolve.assurance.dto.ChecklistTemplateResponse;
import com.omnisolve.assurance.repository.AssetTypeRepository;
import com.omnisolve.assurance.repository.InspectionChecklistItemRepository;
import com.omnisolve.assurance.repository.InspectionChecklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InspectionChecklistService {

    private static final Logger log = LoggerFactory.getLogger(InspectionChecklistService.class);

    private final InspectionChecklistRepository checklistRepository;
    private final InspectionChecklistItemRepository checklistItemRepository;
    private final AssetTypeRepository assetTypeRepository;

    public InspectionChecklistService(
            InspectionChecklistRepository checklistRepository,
            InspectionChecklistItemRepository checklistItemRepository,
            AssetTypeRepository assetTypeRepository) {
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.assetTypeRepository = assetTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> listChecklists() {
        return checklistRepository.findAll().stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistTemplateResponse getChecklist(Long id) {
        InspectionChecklist checklist = findChecklist(id);
        return toTemplateResponse(checklist);
    }

    @Transactional
    public ChecklistTemplateResponse createChecklist(ChecklistRequest request) {
        AssetType assetType = null;
        if (request.assetTypeId() != null) {
            assetType = assetTypeRepository.findById(request.assetTypeId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));
        }

        InspectionChecklist checklist = new InspectionChecklist();
        checklist.setName(request.name());
        checklist.setDescription(request.description());
        checklist.setAssetType(assetType);

        InspectionChecklist saved = checklistRepository.save(checklist);
        log.info("Checklist created: id={}, name={}", saved.getId(), saved.getName());
        return toTemplateResponse(saved);
    }

    @Transactional
    public ChecklistTemplateResponse updateChecklist(Long id, ChecklistRequest request) {
        InspectionChecklist checklist = findChecklist(id);

        AssetType assetType = null;
        if (request.assetTypeId() != null) {
            assetType = assetTypeRepository.findById(request.assetTypeId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Asset type not found"));
        }

        checklist.setName(request.name());
        checklist.setDescription(request.description());
        checklist.setAssetType(assetType);

        return toTemplateResponse(checklistRepository.save(checklist));
    }

    @Transactional
    public void deleteChecklist(Long id) {
        InspectionChecklist checklist = findChecklist(id);
        List<InspectionChecklistItem> items = checklistItemRepository.findByChecklistIdOrderBySortOrder(id);
        checklistItemRepository.deleteAll(items);
        checklistRepository.delete(checklist);
        log.info("Checklist deleted: id={}", id);
    }

    @Transactional
    public ChecklistItemResponse addChecklistItem(Long checklistId, ChecklistItemRequest request) {
        InspectionChecklist checklist = findChecklist(checklistId);

        InspectionChecklistItem item = new InspectionChecklistItem();
        item.setChecklist(checklist);
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setSortOrder(request.sortOrder());

        InspectionChecklistItem saved = checklistItemRepository.save(item);
        return new ChecklistItemResponse(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getSortOrder());
    }

    @Transactional
    public ChecklistItemResponse updateChecklistItem(Long checklistId, Long itemId, ChecklistItemRequest request) {
        findChecklist(checklistId);
        InspectionChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Checklist item not found"));

        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setSortOrder(request.sortOrder());

        InspectionChecklistItem saved = checklistItemRepository.save(item);
        return new ChecklistItemResponse(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getSortOrder());
    }

    @Transactional
    public void deleteChecklistItem(Long checklistId, Long itemId) {
        findChecklist(checklistId);
        InspectionChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Checklist item not found"));
        checklistItemRepository.delete(item);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private InspectionChecklist findChecklist(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Checklist not found"));
    }

    private ChecklistTemplateResponse toTemplateResponse(InspectionChecklist cl) {
        List<ChecklistItemResponse> items = checklistItemRepository
                .findByChecklistIdOrderBySortOrder(cl.getId()).stream()
                .map(i -> new ChecklistItemResponse(i.getId(), i.getTitle(), i.getDescription(), i.getSortOrder()))
                .toList();
        Long assetTypeId = cl.getAssetType() != null ? cl.getAssetType().getId() : null;
        String assetTypeName = cl.getAssetType() != null ? cl.getAssetType().getName() : null;
        return new ChecklistTemplateResponse(cl.getId(), cl.getName(), cl.getDescription(), assetTypeId, assetTypeName, items);
    }
}
