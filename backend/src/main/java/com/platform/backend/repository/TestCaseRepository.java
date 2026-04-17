package com.platform.backend.repository;

import com.platform.backend.entity.TestCase;
import com.platform.backend.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemId(Long problemId);
}
