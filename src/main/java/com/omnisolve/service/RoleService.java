package com.omnisolve.service;

import com.omnisolve.domain.Employee;
import com.omnisolve.domain.Organisation;
import com.omnisolve.domain.Permission;
import com.omnisolve.domain.Role;
import com.omnisolve.domain.RolePermission;
import com.omnisolve.repository.EmployeeRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.repository.PermissionRepository;
import com.omnisolve.repository.RolePermissionRepository;
import com.omnisolve.repository.RoleRepository;
import com.omnisolve.security.AuthenticationUtil;
import com.omnisolve.service.dto.RoleRequest;
import com.omnisolve.service.dto.RoleResponse;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganisationRepository organisationRepository;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            EmployeeRepository employeeRepository,
            OrganisationRepository organisationRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.employeeRepository = employeeRepository;
        this.organisationRepository = organisationRepository;
    }

    private Long getAuthenticatedUserOrganisationId() {
        String userId = AuthenticationUtil.getAuthenticatedUserId();
        log.debug("Retrieving organisation for authenticated user: userId={}", userId);

        Employee authenticatedEmployee = employeeRepository.findByCognitoSub(userId)
                .orElseThrow(() -> {
                    log.warn("Authenticated user not found in employees table: userId={}", userId);
                    return new ResponseStatusException(FORBIDDEN, "User not associated with any organisation");
                });

        Long organisationId = authenticatedEmployee.getOrganisation().getId();
        log.debug("Authenticated user organisation: userId={}, organisationId={}", userId, organisationId);
        return organisationId;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Fetching roles for organisation: organisationId={}", organisationId);

        List<Role> roles = roleRepository.findByOrganisationId(organisationId);
        log.info("Retrieved {} roles for organisation: organisationId={}", roles.size(), organisationId);

        return roles.stream()
                .map(role -> toResponse(role, organisationId))
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Creating role: name={}, organisationId={}", request.name(), organisationId);

        try {
            String roleName = normalizeRoleName(request.name());

            Organisation organisation = organisationRepository.findById(organisationId)
                    .orElseThrow(() -> {
                        log.error("Organisation not found: organisationId={}", organisationId);
                        return new ResponseStatusException(NOT_FOUND, "Organisation not found");
                    });

            if (roleRepository.existsByOrganisationIdAndName(organisationId, roleName)) {
                log.warn("Role name already exists in organisation: organisationId={}, roleName={}", organisationId, roleName);
                throw new ResponseStatusException(CONFLICT, "Role with this name already exists");
            }

            OffsetDateTime now = OffsetDateTime.now();

            Role role = new Role();
            role.setOrganisation(organisation);
            role.setName(roleName);
            role.setDescription(request.description());
            role.setCreatedAt(now);
            role.setUpdatedAt(now);

            Role savedRole = roleRepository.save(role);
            replacePermissions(savedRole, request.permissions());

            log.info("Role created successfully: id={}, name={}, organisationId={}",
                    savedRole.getId(), savedRole.getName(), organisationId);
            return toResponse(savedRole, organisationId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate role name conflict: name={}, organisationId={}", request.name(), organisationId);
            throw new ResponseStatusException(CONFLICT, "Role with this name already exists");
        } catch (Exception e) {
            log.error("Failed to create role: name={}, organisationId={}, error={}",
                    request.name(), organisationId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Updating role: id={}, organisationId={}", id, organisationId);

        try {
            Role role = roleRepository.findByIdAndOrganisationId(id, organisationId)
                    .orElseThrow(() -> {
                        log.warn("Role not found or access denied: id={}, organisationId={}", id, organisationId);
                        return new ResponseStatusException(NOT_FOUND, "Role not found");
                    });

            String roleName = normalizeRoleName(request.name());
            if (roleRepository.existsByOrganisationIdAndNameAndIdNot(organisationId, roleName, id)) {
                log.warn("Role name already exists in organisation: id={}, organisationId={}, roleName={}",
                        id, organisationId, roleName);
                throw new ResponseStatusException(CONFLICT, "Role with this name already exists");
            }

            role.setName(roleName);
            role.setDescription(request.description());
            role.setUpdatedAt(OffsetDateTime.now());

            Role savedRole = roleRepository.save(role);
            replacePermissions(savedRole, request.permissions());

            log.info("Role updated successfully: id={}, name={}, organisationId={}",
                    savedRole.getId(), savedRole.getName(), organisationId);
            return toResponse(savedRole, organisationId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate role name conflict on update: id={}, name={}, organisationId={}",
                    id, request.name(), organisationId);
            throw new ResponseStatusException(CONFLICT, "Role with this name already exists");
        } catch (Exception e) {
            log.error("Failed to update role: id={}, organisationId={}, error={}",
                    id, organisationId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void delete(Long id) {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Deleting role: id={}, organisationId={}", id, organisationId);

        try {
            Role role = roleRepository.findByIdAndOrganisationId(id, organisationId)
                    .orElseThrow(() -> {
                        log.warn("Role not found or access denied: id={}, organisationId={}", id, organisationId);
                        return new ResponseStatusException(NOT_FOUND, "Role not found");
                    });

            rolePermissionRepository.deleteByRole_Id(role.getId());
            roleRepository.delete(role);

            log.info("Role deleted successfully: id={}, organisationId={}", id, organisationId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.warn("Cannot delete role while assigned to employees: id={}, organisationId={}", id, organisationId);
            throw new ResponseStatusException(CONFLICT, "Cannot delete role that is assigned to employees");
        } catch (Exception e) {
            log.error("Failed to delete role: id={}, organisationId={}, error={}",
                    id, organisationId, e.getMessage(), e);
            throw e;
        }
    }

    private void replacePermissions(Role role, List<String> permissionCodes) {
        rolePermissionRepository.deleteByRole_Id(role.getId());

        Set<String> requestedCodes = normalizePermissionCodes(permissionCodes);
        if (requestedCodes.isEmpty()) {
            return;
        }

        List<Permission> permissions = permissionRepository.findByCodeIn(requestedCodes);
        if (permissions.size() != requestedCodes.size()) {
            Set<String> foundCodes = permissions.stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());

            List<String> missingCodes = requestedCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .sorted()
                    .toList();

            throw new ResponseStatusException(BAD_REQUEST,
                    "Invalid permission code(s): " + String.join(", ", missingCodes));
        }

        List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    return rolePermission;
                })
                .toList();

        rolePermissionRepository.saveAll(rolePermissions);
    }

    private RoleResponse toResponse(Role role, Long organisationId) {
        List<String> permissionCodes = rolePermissionRepository.findByRoleId(role.getId()).stream()
                .map(rolePermission -> rolePermission.getPermission().getCode())
                .sorted()
                .toList();

        long userCount = employeeRepository.countByOrganisationIdAndRoleId(organisationId, role.getId());

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                permissionCodes,
                userCount
        );
    }

    private String normalizeRoleName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Role name is required");
        }
        return name.trim();
    }

    private Set<String> normalizePermissionCodes(List<String> permissions) {
        if (permissions == null) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();
        for (String permissionCode : permissions) {
            if (permissionCode == null || permissionCode.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "Permission codes must be non-empty");
            }
            result.add(permissionCode.trim());
        }

        return result;
    }
}

