package com.omnisolve.repository;

import com.omnisolve.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByOrganisationId(Long organisationId);
    
    List<Employee> findByOrganisationIdAndStatus(Long organisationId, String status);
    
    Optional<Employee> findByCognitoSub(String cognitoSub);
    
    Optional<Employee> findByEmail(String email);

    long countByOrganisationIdAndRoleId(Long organisationId, Long roleId);
}
