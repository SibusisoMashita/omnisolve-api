package com.omnisolve.service;

import com.omnisolve.domain.Department;
import com.omnisolve.domain.Employee;
import com.omnisolve.domain.Organisation;
import com.omnisolve.domain.Role;
import com.omnisolve.domain.Site;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.repository.EmployeeRepository;
import com.omnisolve.repository.OrganisationRepository;
import com.omnisolve.repository.RoleRepository;
import com.omnisolve.repository.SiteRepository;
import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.service.dto.CognitoUserResult;
import com.omnisolve.service.dto.EmployeeRequest;
import com.omnisolve.service.dto.EmployeeResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final OrganisationRepository organisationRepository;
    private final SiteRepository siteRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final CognitoService cognitoService;
    private final SecurityContextFacade securityContextFacade;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            OrganisationRepository organisationRepository,
            SiteRepository siteRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            CognitoService cognitoService,
            SecurityContextFacade securityContextFacade
    ) {
        this.employeeRepository = employeeRepository;
        this.organisationRepository = organisationRepository;
        this.siteRepository = siteRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.cognitoService = cognitoService;
        this.securityContextFacade = securityContextFacade;
    }

    /**
     * Activate an employee on their first login if their status is "pending".
     * Called by FirstLoginFilter on every authenticated request.
     *
     * @param cognitoSub The Cognito sub claim from the JWT
     */
    @Transactional
    public void activateOnFirstLogin(String cognitoSub) {
        employeeRepository.findByCognitoSub(cognitoSub).ifPresent(employee -> {
            if ("pending".equals(employee.getStatus())) {
                log.info("First login detected — activating employee: id={}, email={}, cognitoSub={}",
                        employee.getId(), employee.getEmail(), cognitoSub);
                employee.setStatus("active");
                employee.setUpdatedAt(OffsetDateTime.now());
                employeeRepository.save(employee);
                log.info("Employee activated successfully: id={}, email={}", employee.getId(), employee.getEmail());
            }
        });
    }

    /**
     * Get the authenticated user's organisation ID.
     * Delegates to {@link SecurityContextFacade} which consolidates all user/tenant resolution.
     */
    private Long getAuthenticatedUserOrganisationId() {
        return securityContextFacade.currentUser().organisationId();
    }

    /**
     * Verify that an employee belongs to the authenticated user's organisation.
     *
     * @param employee The employee to verify
     */
    private void verifyOrganisationAccess(Employee employee) {
        Long authenticatedOrgId = getAuthenticatedUserOrganisationId();
        Long employeeOrgId = employee.getOrganisation().getId();

        if (!authenticatedOrgId.equals(employeeOrgId)) {
            log.warn("Organisation access denied: authenticatedOrgId={}, employeeOrgId={}", 
                    authenticatedOrgId, employeeOrgId);
            throw new ResponseStatusException(FORBIDDEN, "Access denied to employee from different organisation");
        }
    }

    /**
     * List all employees in the authenticated user's organisation.
     *
     * @return List of employees
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Fetching employees for organisation: organisationId={}", organisationId);

        List<Employee> employees = employeeRepository.findByOrganisationId(organisationId);
        log.info("Retrieved {} employees for organisation: organisationId={}", employees.size(), organisationId);

        return employees.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new employee.
     * This will:
     * 1. Create the user in Cognito
     * 2. Save the employee record in the local database
     * 3. Optionally add the user to a Cognito group based on their role
     *
     * @param request The employee creation request
     * @return The created employee
     */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Long organisationId = getAuthenticatedUserOrganisationId();
        log.info("Creating employee: email={}, organisationId={}", request.email(), organisationId);

        try {
            // Verify organisation exists
            Organisation organisation = organisationRepository.findById(organisationId)
                    .orElseThrow(() -> {
                        log.error("Organisation not found: organisationId={}", organisationId);
                        return new ResponseStatusException(NOT_FOUND, "Organisation not found");
                    });

            // Check if email already exists
            if (employeeRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Employee with email already exists: email={}", request.email());
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Employee with this email already exists"
                );
            }

            // Create user in Cognito
            CognitoUserResult cognitoResult = cognitoService.createUser(
                    request.email(),
                    request.firstName(),
                    request.lastName()
            );

            // Create employee entity
            Employee employee = new Employee();
            employee.setEmail(request.email());
            employee.setFirstName(request.firstName());
            employee.setLastName(request.lastName());
            employee.setOrganisation(organisation);
            employee.setCognitoUsername(cognitoResult.username());
            employee.setCognitoSub(cognitoResult.sub());
            employee.setStatus("active"); // Active immediately — Cognito invitation handles password setup
            employee.setCreatedAt(OffsetDateTime.now());
            employee.setUpdatedAt(OffsetDateTime.now());

            if (request.roleId() != null) {
                Role role = roleRepository.findByIdAndOrganisationId(request.roleId(), organisationId)
                        .orElseThrow(() -> {
                            log.warn("Role not found or access denied: roleId={}, organisationId={}",
                                    request.roleId(), organisationId);
                            return new ResponseStatusException(NOT_FOUND, "Role not found");
                        });
                employee.setRole(role);
            }

            // Set department if provided
            if (request.departmentId() != null) {
                Department department = departmentRepository.findById(request.departmentId())
                        .orElseThrow(() -> {
                            log.warn("Department not found: departmentId={}", request.departmentId());
                            return new ResponseStatusException(NOT_FOUND, "Department not found");
                        });
                employee.setDepartment(department);
            }

            // Set site if provided (must belong to same organisation)
            if (request.siteId() != null) {
                Site site = siteRepository.findById(request.siteId())
                        .orElseThrow(() -> {
                            log.warn("Site not found: siteId={}", request.siteId());
                            return new ResponseStatusException(NOT_FOUND, "Site not found");
                        });

                // Verify site belongs to the same organisation
                if (!site.getOrganisation().getId().equals(organisationId)) {
                    log.warn("Site belongs to different organisation: siteId={}, siteOrgId={}, userOrgId={}",
                            request.siteId(), site.getOrganisation().getId(), organisationId);
                    throw new ResponseStatusException(FORBIDDEN, "Site does not belong to your organisation");
                }

                employee.setSite(site);
            }

            Employee saved = employeeRepository.save(employee);
            log.info("Employee created successfully: id={}, email={}, cognitoUsername={}, cognitoSub={}, roleId={}",
                    saved.getId(), saved.getEmail(), saved.getCognitoUsername(), saved.getCognitoSub(),
                    saved.getRole() != null ? saved.getRole().getId() : null);

            return toResponse(saved);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create employee: email={}, error={}", request.email(), e.getMessage(), e);
            throw new RuntimeException("Failed to create employee: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing employee.
     * Only updates local database fields, not Cognito attributes.
     *
     * @param id      The employee ID
     * @param request The update request
     * @return The updated employee
     */
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        log.info("Updating employee: id={}", id);

        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Employee not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Employee not found");
                    });

            // Verify organisation access
            verifyOrganisationAccess(employee);

            // Update fields
            employee.setFirstName(request.firstName());
            employee.setLastName(request.lastName());
            employee.setUpdatedAt(OffsetDateTime.now());

            Long organisationId = employee.getOrganisation().getId();
            if (request.roleId() != null) {
                Role role = roleRepository.findByIdAndOrganisationId(request.roleId(), organisationId)
                        .orElseThrow(() -> {
                            log.warn("Role not found or access denied: roleId={}, organisationId={}",
                                    request.roleId(), organisationId);
                            return new ResponseStatusException(NOT_FOUND, "Role not found");
                        });
                employee.setRole(role);
            }

            // Update department if provided
            if (request.departmentId() != null) {
                Department department = departmentRepository.findById(request.departmentId())
                        .orElseThrow(() -> {
                            log.warn("Department not found: departmentId={}", request.departmentId());
                            return new ResponseStatusException(NOT_FOUND, "Department not found");
                        });
                employee.setDepartment(department);
            } else {
                employee.setDepartment(null);
            }

            // Update site if provided
            if (request.siteId() != null) {
                Site site = siteRepository.findById(request.siteId())
                        .orElseThrow(() -> {
                            log.warn("Site not found: siteId={}", request.siteId());
                            return new ResponseStatusException(NOT_FOUND, "Site not found");
                        });

                // Verify site belongs to the same organisation
                if (!site.getOrganisation().getId().equals(organisationId)) {
                    log.warn("Site belongs to different organisation: siteId={}, siteOrgId={}, employeeOrgId={}",
                            request.siteId(), site.getOrganisation().getId(), organisationId);
                    throw new ResponseStatusException(FORBIDDEN, "Site does not belong to your organisation");
                }

                employee.setSite(site);
            } else {
                employee.setSite(null);
            }

            Employee saved = employeeRepository.save(employee);
            log.info("Employee updated successfully: id={}, email={}", saved.getId(), saved.getEmail());

            return toResponse(saved);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update employee: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update employee status.
     * This will also enable/disable the user in Cognito.
     *
     * @param id     The employee ID
     * @param status The new status (active/inactive)
     * @return The updated employee
     */
    @Transactional
    public EmployeeResponse updateStatus(Long id, String status) {
        log.info("Updating employee status: id={}, status={}", id, status);

        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Employee not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Employee not found");
                    });

            // Verify organisation access
            verifyOrganisationAccess(employee);

            // Validate status
            if (!status.equals("active") && !status.equals("inactive") && !status.equals("pending")) {
                log.warn("Invalid status value: status={}", status);
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Invalid status. Must be 'active', 'inactive', or 'pending'"
                );
            }

            // Update Cognito user status if cognitoUsername is set
            if (employee.getCognitoUsername() != null) {
                if (status.equals("active")) {
                    cognitoService.enableUser(employee.getCognitoUsername());
                } else if (status.equals("inactive")) {
                    cognitoService.disableUser(employee.getCognitoUsername());
                }
            }

            // Update local status
            employee.setStatus(status);
            employee.setUpdatedAt(OffsetDateTime.now());

            Employee saved = employeeRepository.save(employee);
            log.info("Employee status updated successfully: id={}, status={}", saved.getId(), saved.getStatus());

            return toResponse(saved);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update employee status: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Convert Employee entity to EmployeeResponse DTO.
     *
     * @param employee The employee entity
     * @return The employee response DTO
     */
    private EmployeeResponse toResponse(Employee employee) {
        Long roleId = employee.getRole() != null ? employee.getRole().getId() : null;
        String roleName = employee.getRole() != null ? employee.getRole().getName() : null;

        return new EmployeeResponse(
                employee.getId(),
                employee.getCognitoSub(),
                employee.getCognitoUsername(),
                employee.getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                roleId,
                roleName,
                employee.getDepartment() != null ? employee.getDepartment().getId() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getOrganisation().getId(),
                employee.getOrganisation().getName(),
                employee.getSite() != null ? employee.getSite().getId() : null,
                employee.getSite() != null ? employee.getSite().getName() : null,
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
