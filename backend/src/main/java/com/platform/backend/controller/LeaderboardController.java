/*package com.platform.backend.controller;

import com.platform.backend.dto.UserScoreDTO;
import com.platform.backend.entity.Submission;
import com.platform.backend.repository.SubmissionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final SubmissionRepository submissionRepository;

    public LeaderboardController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
public List<UserScoreDTO> leaderboard() {
    return submissionRepository.getLeaderboard();
}
}*/

package com.platform.backend.controller;

import java.util.*;
import java.util.stream.Collectors;

import com.platform.backend.entity.Submission;
import com.platform.backend.repository.SubmissionRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leaderboard")
@CrossOrigin(origins = "http://localhost:3000")
public class LeaderboardController {

    private final SubmissionRepository submissionRepository;

    public LeaderboardController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/{problemId}")
    public List<Submission> getLeaderboard(@PathVariable Long problemId) {

        List<Submission> submissions =
                submissionRepository.findByProblemId(problemId);

        Map<Long, Submission> bestPerUser = new HashMap<>();

        for (Submission s : submissions) {
            Long userId = s.getUser().getId();

            if (!bestPerUser.containsKey(userId) ||
                s.getScore() > bestPerUser.get(userId).getScore()) {

                bestPerUser.put(userId, s);
            }
        }

        return bestPerUser.values()
                .stream()
                .sorted((a, b) -> {
                    if (b.getScore() != a.getScore()) {
                        return b.getScore() - a.getScore();
                    }
                    return a.getPassedCount() - b.getPassedCount(); // tie-break
                })
                .collect(Collectors.toList());
    }
}
