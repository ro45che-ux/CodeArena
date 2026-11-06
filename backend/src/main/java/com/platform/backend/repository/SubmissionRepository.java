package com.platform.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.platform.backend.dto.UserScoreDTO;
import com.platform.backend.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByProblemId(Long problemId);

    @Query("""
        SELECT new com.platform.backend.dto.UserScoreDTO(
            s.user.id,
            COUNT(s)
        )
        FROM Submission s
        WHERE s.passed = true
        GROUP BY s.user.id
    """)
    List<UserScoreDTO> getLeaderboard();
}
