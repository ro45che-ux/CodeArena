package com.platform.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.entity.Problem;
import com.platform.backend.repository.ProblemRepository;

@RestController
@RequestMapping("/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;

    public ProblemController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /** Admin only (see SecurityConfig). */
    @PostMapping
    public Problem createProblem(@RequestBody Problem problem) {
        if (problem.getTitle() == null || problem.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        problem.setId(null); // always create, never overwrite an existing problem
        return problemRepository.save(problem);
    }

    @GetMapping
    public List<Problem> getProblems() {
        return problemRepository.findAll();
    }
}
