package com.platform.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.backend.dto.ProgressDTO;
import com.platform.backend.dto.SubmissionDTO;
import com.platform.backend.entity.Problem;
import com.platform.backend.entity.User;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.SubmissionRepository;

@RestController
@RequestMapping("/me")
public class ProfileController {

    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public ProfileController(SubmissionRepository submissionRepository, ProblemRepository problemRepository) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
    }

    /** The logged-in user's progress: solved/attempted problems, difficulty breakdown, recent activity. */
    @GetMapping("/progress")
    public ProgressDTO progress(@AuthenticationPrincipal User user) {
        List<Problem> problems = problemRepository.findAll();
        List<Long> solved = submissionRepository.findSolvedProblemIds(user.getId());
        List<Long> attempted = submissionRepository.findAttemptedProblemIds(user.getId());
        Set<Long> solvedSet = Set.copyOf(solved);

        Map<String, Integer> totalByDifficulty = emptyCounts();
        Map<String, Integer> solvedByDifficulty = emptyCounts();
        for (Problem p : problems) {
            String d = p.getDifficulty() == null ? "EASY" : p.getDifficulty().toUpperCase();
            totalByDifficulty.merge(d, 1, Integer::sum);
            if (solvedSet.contains(p.getId())) {
                solvedByDifficulty.merge(d, 1, Integer::sum);
            }
        }

        long total = submissionRepository.countByUserId(user.getId());
        long accepted = submissionRepository.countByUserIdAndPassedTrue(user.getId());

        return new ProgressDTO(
                user.getName(),
                solved.size(),
                problems.size(),
                solved,
                attempted,
                solvedByDifficulty,
                totalByDifficulty,
                total,
                accepted,
                total == 0 ? 0 : (int) Math.round(100.0 * accepted / total),
                submissionRepository.findTop10ByUserIdOrderByIdDesc(user.getId()).stream()
                        .map(SubmissionDTO::summary).toList());
    }

    private static Map<String, Integer> emptyCounts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        DIFFICULTIES.forEach(d -> m.put(d, 0));
        return m;
    }
}
