package com.platform.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.backend.entity.Submission;
import com.platform.backend.entity.User;
import com.platform.backend.service.SubmissionService;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * The submitting user always comes from the verified JWT, never from the URL. The legacy
     * {@code /{problemId}/{userId}} form is still accepted so older front-ends keep working, but
     * the {@code userId} segment is ignored.
     */
    @PostMapping({"/{problemId}", "/{problemId}/{legacyUserId}"})
    public Submission submit(@PathVariable Long problemId,
                             @RequestBody String code,
                             @AuthenticationPrincipal User user) {
        return submissionService.submit(user, problemId, code);
    }
}
