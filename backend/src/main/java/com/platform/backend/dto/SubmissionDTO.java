package com.platform.backend.dto;

import java.time.Instant;

import com.platform.backend.entity.Submission;
import com.platform.backend.entity.SubmissionStatus;

/**
 * A user's own submission. {@code code} and {@code output} are only filled in for the detail view,
 * not for list views.
 */
public record SubmissionDTO(
        Long id,
        Long problemId,
        String problemTitle,
        SubmissionStatus status,
        boolean passed,
        int passedCount,
        int totalCount,
        int score,
        long executionTimeMs,
        Instant createdAt,
        String code,
        String output) {

    public static SubmissionDTO summary(Submission s) {
        return of(s, false);
    }

    public static SubmissionDTO detail(Submission s) {
        return of(s, true);
    }

    private static SubmissionDTO of(Submission s, boolean withCode) {
        return new SubmissionDTO(
                s.getId(),
                s.getProblem() != null ? s.getProblem().getId() : null,
                s.getProblem() != null ? s.getProblem().getTitle() : null,
                s.getStatus(),
                s.isPassed(),
                s.getPassedCount(),
                s.getTotalCount(),
                s.getScore(),
                s.getExecutionTimeMs(),
                s.getCreatedAt(),
                withCode ? s.getCode() : null,
                withCode ? s.getUserOutput() : null);
    }
}
