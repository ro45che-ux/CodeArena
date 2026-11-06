package com.platform.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.backend.entity.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdOrderByIdAsc(Long problemId);

    List<TestCase> findByProblemId(Long problemId);
}
