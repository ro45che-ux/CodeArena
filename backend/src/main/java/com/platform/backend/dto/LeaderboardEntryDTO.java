package com.platform.backend.dto;

import com.platform.backend.entity.Submission;
import com.platform.backend.entity.SubmissionStatus;

/**
 * Public leaderboard row. Deliberately contains no source code, e-mail or other user data.
 */
public record LeaderboardEntryDTO(
        String userName,
        int score,
        int passedCount,
        int totalCount,
        long executionTimeMs,
        SubmissionStatus status) {

    public static LeaderboardEntryDTO from(Submission s) {
        return new LeaderboardEntryDTO(
                s.getUser() != null ? s.getUser().getName() : "Unknown",
                s.getScore(),
                s.getPassedCount(),
                s.getTotalCount(),
                s.getExecutionTimeMs(),
                s.getStatus());
    }
}
