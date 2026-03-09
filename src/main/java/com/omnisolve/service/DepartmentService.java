package com.omnisolve.service;

import com.omnisolve.domain.Department;
import com.omnisolve.repository.DepartmentRepository;
import com.omnisolve.service.dto.DepartmentRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    public List<Department> list() {
        return repository.findAll();
    }

    @Transactional
    public Department create(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.name());
        department.setDescription(request.description());
        return repository.save(department);
    }

    @Transactional
    public Department update(Long id, DepartmentRequest request) {
        Department department = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Department not found"));
        department.setName(request.name());
        department.setDescription(request.description());
        return repository.save(department);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

