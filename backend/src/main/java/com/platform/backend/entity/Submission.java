package com.platform.backend.entity;

import jakarta.persistence.*;

@Entity
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userOutput;
    private String code;
    private boolean passed;
    private SubmissionStatus status;
    @ManyToOne
    private User user;

    @ManyToOne
    private Problem problem;
    private int score;
    // getters setters
    private int passedCount;
private int totalCount;
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserOutput() {
        return userOutput;
    }

    public void setUserOutput(String userOutput) {
        this.userOutput = userOutput;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    public int getPassedCount() {
        return passedCount;
    }
    
    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    public SubmissionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
    
}

