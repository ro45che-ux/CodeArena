package com.platform.backend.dto;

public class UserScoreDTO {

    private Long userId;
    private Long solved;

    public UserScoreDTO(Long userId, Long solved) {
        this.userId = userId;
        this.solved = solved;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSolved() {
        return solved;
    }
}
