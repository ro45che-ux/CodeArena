package com.platform.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.platform.backend.entity.Problem;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}

