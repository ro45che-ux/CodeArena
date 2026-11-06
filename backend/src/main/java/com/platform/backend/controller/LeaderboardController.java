package com.platform.backend.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.backend.dto.LeaderboardEntryDTO;
import com.platform.backend.entity.Submission;
import com.platform.backend.repository.SubmissionRepository;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private static final int MAX_ROWS = 100;

    /** Higher score first; for equal scores the faster run wins. */
    private static final Comparator<Submission> RANKING =
            Comparator.comparingInt(Submission::getScore).reversed()
                    .thenComparingLong(Submission::getExecutionTimeMs);

    private final SubmissionRepository submissionRepository;

    public LeaderboardController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/{problemId}")
    public List<LeaderboardEntryDTO> getLeaderboard(@PathVariable Long problemId) {

        Map<Long, Submission> bestPerUser = new HashMap<>();
        for (Submission s : submissionRepository.findByProblemId(problemId)) {
            if (s.getUser() == null) {
                continue;
            }
            bestPerUser.merge(s.getUser().getId(), s,
                    (a, b) -> RANKING.compare(a, b) <= 0 ? a : b);
        }

        return bestPerUser.values().stream()
                .sorted(RANKING)
                .limit(MAX_ROWS)
                .map(LeaderboardEntryDTO::from)
                .toList();
    }
}
