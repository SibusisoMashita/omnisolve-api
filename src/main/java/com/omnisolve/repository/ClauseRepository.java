package com.omnisolve.repository;

import com.omnisolve.domain.Clause;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClauseRepository extends JpaRepository<Clause, Long> {

    List<Clause> findByStandardIdOrderBySortOrderAsc(Long standardId);
}

