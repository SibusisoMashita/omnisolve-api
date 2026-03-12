package com.omnisolve.repository;

import com.omnisolve.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByOrganisationId(Long organisationId);

    Optional<Role> findByIdAndOrganisationId(Long id, Long organisationId);

    boolean existsByOrganisationIdAndName(Long organisationId, String name);

    boolean existsByOrganisationIdAndNameAndIdNot(Long organisationId, String name, Long id);
}

