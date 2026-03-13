package com.omnisolve.repository;

import com.omnisolve.domain.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {
    Optional<Standard> findByCode(String code);
}
