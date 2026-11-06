package com.platform.backend.dto;

/**
 * Public, aggregate-only statistics for one problem.
 *
 * @param acceptanceRate accepted submissions as a whole-number percentage of all submissions
 */
public record ProblemStatsDTO(Long problemId, Long submissions, Long accepted, int acceptanceRate) {

    /** Constructor used by the JPQL query (the rate is derived). */
    public ProblemStatsDTO(Long problemId, Long submissions, Long accepted) {
        this(problemId, submissions, accepted,
                submissions == null || submissions == 0 ? 0 : (int) Math.round(100.0 * accepted / submissions));
    }
}
