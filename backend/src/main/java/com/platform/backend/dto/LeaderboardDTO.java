package com.platform.backend.dto;

public class LeaderboardDTO {

    private String userName;
    private String problemTitle;
    private boolean passed;

    public LeaderboardDTO(String userName, String problemTitle, boolean passed) {
        this.userName = userName;
        this.problemTitle = problemTitle;
        this.passed = passed;
    }

    public String getUserName() {
        return userName;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public boolean isPassed() {
        return passed;
    }
}

