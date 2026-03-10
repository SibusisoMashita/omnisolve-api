package com.omnisolve.service;

import com.omnisolve.domain.Department;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.service.dto.DepartmentRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    public List<Department> list() {
        log.info("Fetching all departments");
        List<Department> departments = repository.findAll();
        log.info("Retrieved {} departments", departments.size());
        return departments;
    }

    @Transactional
    public Department create(DepartmentRequest request) {
        log.info("Creating department: name={}", request.name());
        try {
            Department department = new Department();
            department.setName(request.name());
            department.setDescription(request.description());
            Department saved = repository.save(department);
            log.info("Department created successfully: id={}, name={}", saved.getId(), saved.getName());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create department: name={}, error={}", request.name(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public Department update(Long id, DepartmentRequest request) {
        log.info("Updating department: id={}, name={}", id, request.name());
        try {
            Department department = repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Department not found: id={}", id);
                        return new ResponseStatusException(NOT_FOUND, "Department not found");
                    });
            department.setName(request.name());
            department.setDescription(request.description());
            Department saved = repository.save(department);
            log.info("Department updated successfully: id={}, name={}", saved.getId(), saved.getName());
            return saved;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update department: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting department: id={}", id);
        try {
            repository.deleteById(id);
            log.info("Department deleted successfully: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete department: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }
}

