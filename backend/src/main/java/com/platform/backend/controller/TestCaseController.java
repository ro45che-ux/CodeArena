package com.platform.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.backend.entity.Problem;
import com.platform.backend.entity.TestCase;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.TestCaseRepository;

@RestController
@RequestMapping("/testcases")
public class TestCaseController {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseController(TestCaseRepository testCaseRepository,
                              ProblemRepository problemRepository) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    /** Add a test case to a problem. Admin only (see SecurityConfig). */
    @PostMapping("/{problemId}")
    public TestCase addTestCase(@PathVariable Long problemId, @RequestBody TestCase testCase) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem not found"));
        testCase.setId(null);
        testCase.setProblem(problem);
        return testCaseRepository.save(testCase);
    }

    /** Public: only the non-hidden test cases, shown as examples on the problem page. */
    @GetMapping("/{problemId}")
    public List<Map<String, String>> getExampleTestCases(@PathVariable Long problemId) {
        return testCaseRepository.findByProblemIdOrderByIdAsc(problemId).stream()
                .filter(tc -> !tc.isHidden())
                .map(tc -> Map.of(
                        "input", tc.getInput() != null ? tc.getInput() : "",
                        "expectedOutput", tc.getExpectedOutput() != null ? tc.getExpectedOutput() : ""))
                .toList();
    }
}
