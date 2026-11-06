package com.platform.backend.dto;

import com.platform.backend.entity.Submission;
import com.platform.backend.entity.SubmissionStatus;

/** Result of a "Run" (example test cases only). Nothing about it is stored. */
public record JudgeResultDTO(
        SubmissionStatus status,
        int passedCount,
        int totalCount,
        int score,
        long executionTimeMs,
        String message) {

    public static JudgeResultDTO from(Submission s) {
        return new JudgeResultDTO(s.getStatus(), s.getPassedCount(), s.getTotalCount(),
                s.getScore(), s.getExecutionTimeMs(), s.getMessage());
    }
}
