/*package com.platform.backend.repository;

import com.platform.backend.dto.UserScoreDTO;
import com.platform.backend.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

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
}*/
package com.platform.backend.repository;

import com.platform.backend.dto.UserScoreDTO;
import com.platform.backend.entity.Submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // ✅ SIMPLE METHOD (used for service/controller logic)
    List<Submission> findByProblemId(Long problemId);


    // 🔥 ADVANCED LEADERBOARD (BEST SCORE PER USER)
    /*@Query("""
        SELECT new com.platform.backend.dto.UserScoreDTO(
            s.user.id,
            MAX(s.score)
        )
        FROM Submission s
        WHERE s.problem.id = :problemId
        GROUP BY s.user.id
        ORDER BY MAX(s.score) DESC
    """)
    List<UserScoreDTO> getLeaderboardByProblem(@Param("problemId") Long problemId);*/


    // ⚠️ OLD METHOD (keep if needed, but not ideal)
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
