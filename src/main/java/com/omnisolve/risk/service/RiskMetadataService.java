package com.omnisolve.risk.service;

import com.omnisolve.risk.repository.RiskCategoryRepository;
import com.omnisolve.risk.repository.RiskLikelihoodRepository;
import com.omnisolve.risk.repository.RiskSeverityRepository;
import com.omnisolve.risk.service.dto.RiskMetadataResponse;
import com.omnisolve.risk.service.dto.RiskOptionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiskMetadataService {

    private final RiskCategoryRepository categoryRepository;
    private final RiskSeverityRepository severityRepository;
    private final RiskLikelihoodRepository likelihoodRepository;

    public RiskMetadataService(
            RiskCategoryRepository categoryRepository,
            RiskSeverityRepository severityRepository,
            RiskLikelihoodRepository likelihoodRepository) {
        this.categoryRepository = categoryRepository;
        this.severityRepository = severityRepository;
        this.likelihoodRepository = likelihoodRepository;
    }

    @Transactional(readOnly = true)
    public RiskMetadataResponse getMetadata() {
        List<RiskOptionDTO> categories = categoryRepository.findAll().stream()
                .map(c -> new RiskOptionDTO(c.getId(), c.getName(), null))
                .toList();

        List<RiskOptionDTO> severities = severityRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .map(s -> new RiskOptionDTO(s.getId(), s.getName(), s.getLevel()))
                .toList();

        List<RiskOptionDTO> likelihoods = likelihoodRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .map(l -> new RiskOptionDTO(l.getId(), l.getName(), l.getLevel()))
                .toList();

        return new RiskMetadataResponse(categories, severities, likelihoods);
    }
}
