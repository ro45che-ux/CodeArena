package com.platform.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.dto.ProblemStatsDTO;
import com.platform.backend.entity.Problem;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.SubmissionRepository;

@RestController
@RequestMapping("/problems")
public class ProblemController {

    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public ProblemController(ProblemRepository problemRepository, SubmissionRepository submissionRepository) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    /** Admin only (see SecurityConfig). */
    @PostMapping
    public Problem createProblem(@RequestBody Problem problem) {
        if (problem.getTitle() == null || problem.getTitle().isBlank() || problem.getTitle().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (max 255 characters)");
        }
        if (problem.getDifficulty() == null || !DIFFICULTIES.contains(problem.getDifficulty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty must be EASY, MEDIUM or HARD");
        }
        if (tooLong(problem.getDescription(), 2000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is too long (max 2000 characters)");
        }
        if (tooLong(problem.getSolutionTemplate(), 5000) || tooLong(problem.getDriverCode(), 5000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template and driver code are limited to 5000 characters");
        }
        problem.setId(null); // always create, never overwrite an existing problem
        return problemRepository.save(problem);
    }

    @GetMapping
    public List<Problem> getProblems() {
        return problemRepository.findAll();
    }

    /** Public aggregate numbers only (how many submissions, how many accepted). */
    @GetMapping("/stats")
    public List<ProblemStatsDTO> stats() {
        return submissionRepository.getProblemStats();
    }

    private static boolean tooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
