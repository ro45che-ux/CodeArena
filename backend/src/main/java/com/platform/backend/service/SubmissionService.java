package com.platform.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.platform.backend.entity.Submission;
import com.platform.backend.entity.TestCase;
import com.platform.backend.repository.TestCaseRepository;

@Service
public class SubmissionService {

    private final TestCaseRepository testCaseRepository;
    private final CodeRunnerService codeRunnerService;

    public SubmissionService(TestCaseRepository testCaseRepository,
                             CodeRunnerService codeRunnerService) {
        this.testCaseRepository = testCaseRepository;
        this.codeRunnerService = codeRunnerService;
    }

    public Submission evaluateSubmission(Submission submission) {

        List<TestCase> testCases = testCaseRepository.findAll()
                .stream()
                .filter(tc -> tc.getProblem().getId()
                        .equals(submission.getProblem().getId()))
                .collect(Collectors.toList());

        int passed = 0;

        for (TestCase tc : testCases) {

            String output = codeRunnerService.runCode(
                    submission.getCode(),
                    tc.getProblem().getDriverCode(),
                    tc.getInput()
            );

            if (output != null &&
                output.trim().equals(tc.getExpectedOutput().trim())) {
                passed++;
            }
            submission.setUserOutput(output);
        }

        int total = testCases.size();

        submission.setPassedCount(passed);
        submission.setTotalCount(total);
        submission.setScore(passed * 10);
        submission.setPassed(passed == total);

        return submission;
    }
}