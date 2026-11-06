package com.platform.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.dto.JudgeResultDTO;
import com.platform.backend.dto.SubmissionDTO;
import com.platform.backend.entity.Submission;
import com.platform.backend.entity.User;
import com.platform.backend.repository.SubmissionRepository;
import com.platform.backend.service.SubmissionService;

/** Everything here requires a login; the user always comes from the verified JWT. */
@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;

    public SubmissionController(SubmissionService submissionService, SubmissionRepository submissionRepository) {
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
    }

    /** Graded submission: all test cases, stored, counts for the leaderboard. */
    @PostMapping("/{problemId}")
    public Submission submit(@PathVariable Long problemId,
                             @RequestBody String code,
                             @AuthenticationPrincipal User user) {
        return submissionService.submit(user, problemId, code);
    }

    /** Quick run against the example test cases only; nothing is stored. */
    @PostMapping("/{problemId}/run")
    public JudgeResultDTO run(@PathVariable Long problemId,
                              @RequestBody String code,
                              @AuthenticationPrincipal User user) {
        return JudgeResultDTO.from(submissionService.run(user, problemId, code));
    }

    /** The caller's own submissions, newest first (optionally for one problem). No source code. */
    @GetMapping("/mine")
    public List<SubmissionDTO> mine(@RequestParam(required = false) Long problemId,
                                    @AuthenticationPrincipal User user) {
        List<Submission> rows = problemId != null
                ? submissionRepository.findTop50ByUserIdAndProblemIdOrderByIdDesc(user.getId(), problemId)
                : submissionRepository.findTop100ByUserIdOrderByIdDesc(user.getId());
        return rows.stream().map(SubmissionDTO::summary).toList();
    }

    /** One of the caller's own submissions including its source code. Other users' ids give 404. */
    @GetMapping("/{id}")
    public SubmissionDTO detail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return submissionRepository.findByIdAndUserId(id, user.getId())
                .map(SubmissionDTO::detail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }
}
