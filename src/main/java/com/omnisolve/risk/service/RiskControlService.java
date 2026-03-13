package com.omnisolve.risk.service;

import com.omnisolve.audit.Auditable;
import com.omnisolve.risk.domain.Risk;
import com.omnisolve.risk.domain.RiskControl;
import com.omnisolve.risk.repository.RiskControlRepository;
import com.omnisolve.risk.repository.RiskRepository;
import com.omnisolve.risk.service.dto.RiskControlDTO;
import com.omnisolve.risk.service.dto.RiskControlRequest;
import com.omnisolve.risk.service.dto.RiskControlUpdateRequest;
import com.omnisolve.security.SecurityContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskControlService {

    private static final Logger log = LoggerFactory.getLogger(RiskControlService.class);

    private final RiskControlRepository controlRepository;
    private final RiskRepository riskRepository;
    private final SecurityContextFacade securityContextFacade;

    public RiskControlService(
            RiskControlRepository controlRepository,
            RiskRepository riskRepository,
            SecurityContextFacade securityContextFacade) {
        this.controlRepository = controlRepository;
        this.riskRepository = riskRepository;
        this.securityContextFacade = securityContextFacade;
    }

    @Transactional(readOnly = true)
    public List<RiskControlDTO> listControls(UUID riskId) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        findRisk(riskId, organisationId);
        return controlRepository.findByRiskIdOrderByCreatedAtAsc(riskId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    @Auditable(action = "RISK_CONTROL_ADDED", entityType = "risk")
    public RiskControlDTO addControl(UUID riskId, RiskControlRequest request) {
        Long organisationId = securityContextFacade.currentUser().organisationId();
        log.info("Adding control to risk: riskId={}, organisationId={}", riskId, organisationId);

        Risk risk = findRisk(riskId, organisationId);

        RiskControl control = new RiskControl();
        control.setRisk(risk);
        control.setDescription(request.description());
        control.setControlOwner(request.controlOwner());
        control.setCreatedAt(OffsetDateTime.now());

        return toDTO(controlRepository.save(control));
    }

    @Transactional
    @Auditable(action = "RISK_CONTROL_UPDATED", entityType = "risk")
    public RiskControlDTO updateControl(Long controlId, RiskControlUpdateRequest request) {
        log.info("Updating risk control: controlId={}", controlId);

        RiskControl control = controlRepository.findById(controlId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk control not found"));

        if (request.description() != null)  control.setDescription(request.description());
        if (request.controlOwner() != null) control.setControlOwner(request.controlOwner());

        return toDTO(controlRepository.save(control));
    }

    @Transactional
    @Auditable(action = "RISK_CONTROL_DELETED", entityType = "risk")
    public void deleteControl(Long controlId) {
        log.info("Deleting risk control: controlId={}", controlId);

        RiskControl control = controlRepository.findById(controlId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk control not found"));
        controlRepository.delete(control);
    }

    private Risk findRisk(UUID riskId, Long organisationId) {
        return riskRepository.findByIdAndOrganisationId(riskId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk not found"));
    }

    private RiskControlDTO toDTO(RiskControl c) {
        return new RiskControlDTO(c.getId(), c.getRisk().getId(), c.getDescription(),
                c.getControlOwner(), c.getCreatedAt());
    }
}
