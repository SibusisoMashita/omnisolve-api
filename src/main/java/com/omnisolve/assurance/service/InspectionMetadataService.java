package com.omnisolve.assurance.service;

import com.omnisolve.assurance.domain.InspectionSeverity;
import com.omnisolve.assurance.domain.InspectionType;
import com.omnisolve.assurance.domain.Tag;
import com.omnisolve.assurance.dto.InspectionSeverityResponse;
import com.omnisolve.assurance.dto.InspectionTypeResponse;
import com.omnisolve.assurance.dto.TagRequest;
import com.omnisolve.assurance.dto.TagResponse;
import com.omnisolve.assurance.repository.InspectionSeverityRepository;
import com.omnisolve.assurance.repository.InspectionTypeRepository;
import com.omnisolve.assurance.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InspectionMetadataService {

    private static final Logger log = LoggerFactory.getLogger(InspectionMetadataService.class);

    private final InspectionTypeRepository inspectionTypeRepository;
    private final InspectionSeverityRepository inspectionSeverityRepository;
    private final TagRepository tagRepository;

    public InspectionMetadataService(
            InspectionTypeRepository inspectionTypeRepository,
            InspectionSeverityRepository inspectionSeverityRepository,
            TagRepository tagRepository) {
        this.inspectionTypeRepository = inspectionTypeRepository;
        this.inspectionSeverityRepository = inspectionSeverityRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<InspectionTypeResponse> listInspectionTypes() {
        return inspectionTypeRepository.findAll().stream()
                .map(t -> new InspectionTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionSeverityResponse> listInspectionSeverities() {
        return inspectionSeverityRepository.findAllByOrderByLevelAsc().stream()
                .map(s -> new InspectionSeverityResponse(s.getId(), s.getName(), s.getLevel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(t -> new TagResponse(t.getId(), t.getName(), t.getCategory()))
                .toList();
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        Tag tag = new Tag();
        tag.setName(request.name());
        tag.setCategory(request.category());
        Tag saved = tagRepository.save(tag);
        log.info("Tag created: id={}, name={}", saved.getId(), saved.getName());
        return new TagResponse(saved.getId(), saved.getName(), saved.getCategory());
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tag not found"));
        tagRepository.delete(tag);
        log.info("Tag deleted: id={}", id);
    }
}
