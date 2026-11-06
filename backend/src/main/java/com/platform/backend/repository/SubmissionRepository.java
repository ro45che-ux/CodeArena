package com.platform.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.platform.backend.dto.ProblemStatsDTO;
import com.platform.backend.dto.UserScoreDTO;
import com.platform.backend.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByProblemId(Long problemId);

    // ---- a user's own history -------------------------------------------------
    List<Submission> findTop50ByUserIdAndProblemIdOrderByIdDesc(Long userId, Long problemId);

    List<Submission> findTop100ByUserIdOrderByIdDesc(Long userId);

    List<Submission> findTop10ByUserIdOrderByIdDesc(Long userId);

    Optional<Submission> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndPassedTrue(Long userId);

    @Query("select distinct s.problem.id from Submission s where s.user.id = :userId and s.passed = true")
    List<Long> findSolvedProblemIds(@Param("userId") Long userId);

    @Query("select distinct s.problem.id from Submission s where s.user.id = :userId")
    List<Long> findAttemptedProblemIds(@Param("userId") Long userId);

    // ---- aggregates ------------------------------------------------------------
    @Query("""
        SELECT new com.platform.backend.dto.ProblemStatsDTO(
            s.problem.id,
            COUNT(s),
            SUM(CASE WHEN s.passed = true THEN 1L ELSE 0L END)
        )
        FROM Submission s
        GROUP BY s.problem.id
    """)
    List<ProblemStatsDTO> getProblemStats();

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
