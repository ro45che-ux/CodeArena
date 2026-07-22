package com.platform.backend.controller;

import org.springframework.web.bind.annotation.*;

import com.platform.backend.entity.TestCase;
import com.platform.backend.entity.Problem;
import com.platform.backend.repository.TestCaseRepository;
import com.platform.backend.repository.ProblemRepository;

@RestController
@RequestMapping("/testcases")
@CrossOrigin(origins = "*")
public class TestCaseController {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseController(TestCaseRepository testCaseRepository,
                              ProblemRepository problemRepository) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    // add testcase to problem
    @PostMapping("/{problemId}")
    public TestCase addTestCase(@PathVariable Long problemId,
                                @RequestBody TestCase testCase) {

        Problem problem = problemRepository.findById(problemId).orElseThrow();
        testCase.setProblem(problem);

        return testCaseRepository.save(testCase);
    }

    // Get non-hidden test cases (examples) for display on problem page
    @GetMapping("/{problemId}")
    public java.util.List<java.util.Map<String, String>> getExampleTestCases(@PathVariable Long problemId) {
        return testCaseRepository.findByProblemId(problemId).stream()
                .filter(tc -> !tc.isHidden())
                .map(tc -> java.util.Map.of(
                        "input", tc.getInput() != null ? tc.getInput() : "",
                        "expectedOutput", tc.getExpectedOutput() != null ? tc.getExpectedOutput() : ""
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}

