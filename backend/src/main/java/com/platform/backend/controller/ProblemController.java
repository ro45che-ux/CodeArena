package com.platform.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.platform.backend.entity.Problem;
import com.platform.backend.repository.ProblemRepository;

@RestController
@RequestMapping("/problems")
@CrossOrigin(origins = "*")
public class ProblemController {

    private final ProblemRepository problemRepository;

    public ProblemController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    // create problem
    @PostMapping
    public Problem createProblem(@RequestBody Problem problem) {
        return problemRepository.save(problem);
    }

    // get all problems
    @GetMapping
    public List<Problem> getProblems() {
        return problemRepository.findAll();
    }
}

