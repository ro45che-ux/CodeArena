package com.platform.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.entity.Problem;
import com.platform.backend.entity.TestCase;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.TestCaseRepository;

@RestController
@RequestMapping("/testcases")
public class TestCaseController {

    /** Matches the length of the input/expected_output columns. */
    private static final int MAX_FIELD_LENGTH = 255;

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
        if (testCase.getExpectedOutput() == null || testCase.getExpectedOutput().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected output is required");
        }
        if (length(testCase.getInput()) > MAX_FIELD_LENGTH || length(testCase.getExpectedOutput()) > MAX_FIELD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Input and expected output are limited to " + MAX_FIELD_LENGTH + " characters");
        }
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

    /** Admin only: every test case of a problem including the hidden ones. */
    @GetMapping("/{problemId}/all")
    public List<TestCase> getAllTestCases(@PathVariable Long problemId) {
        return testCaseRepository.findByProblemIdOrderByIdAsc(problemId);
    }

    /** Admin only: remove one test case by its own id. */
    @DeleteMapping("/case/{testCaseId}")
    public Map<String, String> deleteTestCase(@PathVariable Long testCaseId) {
        if (!testCaseRepository.existsById(testCaseId)) {
            throw new NoSuchElementException("Test case not found");
        }
        testCaseRepository.deleteById(testCaseId);
        return Map.of("status", "deleted");
    }

    private static int length(String s) {
        return s == null ? 0 : s.length();
    }
}
