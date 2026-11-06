package com.platform.backend.dto;

import java.util.List;
import java.util.Map;

/** Everything the profile page and the "solved" markers need, for the logged-in user. */
public record ProgressDTO(
        String name,
        int solvedCount,
        int totalProblems,
        List<Long> solvedProblemIds,
        List<Long> attemptedProblemIds,
        Map<String, Integer> solvedByDifficulty,
        Map<String, Integer> totalByDifficulty,
        long totalSubmissions,
        long acceptedSubmissions,
        int acceptanceRate,
        List<SubmissionDTO> recent) {
}
