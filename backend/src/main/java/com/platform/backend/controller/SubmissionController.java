/*package com.platform.backend.controller;
import java.util.List;
import com.platform.backend.entity.*;
import com.platform.backend.repository.*;
import com.platform.backend.service.CodeRunnerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submissions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeRunnerService runner;

    public SubmissionController(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            TestCaseRepository testCaseRepository,
            CodeRunnerService runner
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.runner = runner;
    }

    @PostMapping("/{problemId}/{userId}")
public Submission submit(@PathVariable Long problemId,
                         @PathVariable Long userId,
                         @RequestBody String code) throws Exception {

    User user = userRepository.findById(userId).orElseThrow();
    Problem problem = problemRepository.findById(problemId).orElseThrow();

    List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);

    int passed = 0;

    for (TestCase tc : testCases) {
        String output = runner.runJava(code, tc.getInput()).trim();

        if (output.equals(tc.getExpectedOutput().trim())) {
            passed++;
        }
    }

    Submission s = new Submission();
    s.setUser(user);
    s.setProblem(problem);
    s.setPassed(passed == testCases.size());
    s.setPassedCount(passed);
    s.setTotalCount(testCases.size());
    s.setUserOutput(code);

    return submissionRepository.save(s);
}

}*/

package com.platform.backend.controller;

import java.util.List;

import com.platform.backend.entity.*;
import com.platform.backend.repository.*;
import com.platform.backend.service.CodeRunnerService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submissions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeRunnerService runner;

    public SubmissionController(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            TestCaseRepository testCaseRepository,
            CodeRunnerService runner
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.runner = runner;
    }

    @PostMapping("/{problemId}/{userId}")
    public Submission submit(@PathVariable Long problemId,
                             @PathVariable Long userId,
                             @RequestBody String code) {

        User user = userRepository.findById(userId).orElseThrow();
        Problem problem = problemRepository.findById(problemId).orElseThrow();

        List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);

        int passed = 0;

        Submission s = new Submission();
        s.setUser(user);
        s.setProblem(problem);
        s.setCode(code); // ✅ FIX (store actual code)

        for (TestCase tc : testCases) {

            try {
                String output = runner.runCode(code, tc.getInput());

                if (output != null &&
                    output.trim().equals(tc.getExpectedOutput().trim())) {
                    passed++;
                }

            } catch (Exception e) {
                s.setStatus(SubmissionStatus.ERROR);
            }
        }

        int total = testCases.size();

        s.setPassedCount(passed);
        s.setTotalCount(total);

        // ✅ SCORE (percentage)
        int score = (passed * 100) / total;
        s.setScore(score);

        // ✅ STATUS LOGIC
        if (s.getStatus() != SubmissionStatus.ERROR) {
            if (passed == total) {
                s.setStatus(SubmissionStatus.ACCEPTED);
            } else {
                s.setStatus(SubmissionStatus.WRONG_ANSWER);
            }
        }

        return submissionRepository.save(s);
    }
}