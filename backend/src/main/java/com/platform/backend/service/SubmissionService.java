package com.platform.backend.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.entity.Problem;
import com.platform.backend.entity.Submission;
import com.platform.backend.entity.SubmissionStatus;
import com.platform.backend.entity.TestCase;
import com.platform.backend.entity.User;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.SubmissionRepository;
import com.platform.backend.repository.TestCaseRepository;

/**
 * Judges a submission: validates it, throttles abusive clients, compiles once, runs every test
 * case and stores the result.
 */
@Service
public class SubmissionService {

    /** Matches the length of the {@code code} column. */
    public static final int MAX_CODE_LENGTH = 5000;
    private static final int MAX_STORED_OUTPUT = 5000;
    private static final long RATE_WINDOW_MS = 60_000;
    private static final long WAIT_FOR_SLOT_SECONDS = 30;

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeRunnerService runner;

    /** Limits how many submissions are compiled/run at the same time (protects the host). */
    private final Semaphore judgeSlots;
    private final int submissionsPerMinute;
    private final int runsPerMinute;

    private final Map<Long, Deque<Long>> recentSubmissions = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Long>> recentRuns = new ConcurrentHashMap<>();
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public SubmissionService(ProblemRepository problemRepository,
                             TestCaseRepository testCaseRepository,
                             SubmissionRepository submissionRepository,
                             CodeRunnerService runner,
                             @Value("${judge.max-concurrent:2}") int maxConcurrent,
                             @Value("${judge.submissions-per-minute:10}") int submissionsPerMinute,
                             @Value("${judge.runs-per-minute:30}") int runsPerMinute) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.runner = runner;
        this.judgeSlots = new Semaphore(Math.max(1, maxConcurrent), true);
        this.submissionsPerMinute = submissionsPerMinute;
        this.runsPerMinute = runsPerMinute;
    }

    /** Judges against ALL test cases (hidden ones included) and stores the result. */
    public Submission submit(User user, Long problemId, String code) {
        validateCode(code);
        Problem problem = findProblem(problemId);

        List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByIdAsc(problemId);
        if (testCases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This problem has no test cases yet");
        }

        enforceRateLimit(user.getId(), recentSubmissions, submissionsPerMinute);
        Submission result = judgeExclusively(user, problem, testCases, code);
        result.setCreatedAt(Instant.now());
        return submissionRepository.save(result);
    }

    /**
     * "Run": judges against the example (non-hidden) test cases only and stores nothing. Meant for
     * trying code quickly without using up a graded submission.
     */
    public Submission run(User user, Long problemId, String code) {
        validateCode(code);
        Problem problem = findProblem(problemId);

        List<TestCase> examples = testCaseRepository.findByProblemIdOrderByIdAsc(problemId).stream()
                .filter(tc -> !tc.isHidden())
                .toList();
        if (examples.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This problem has no example test cases to run against");
        }

        enforceRateLimit(user.getId(), recentRuns, runsPerMinute);
        return judgeExclusively(user, problem, examples, code);
    }

    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code must not be empty");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Code is too long (maximum " + MAX_CODE_LENGTH + " characters)");
        }
    }

    private Problem findProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
    }

    /** One judging job per user at a time, and at most N jobs on the whole server. */
    private Submission judgeExclusively(User user, Problem problem, List<TestCase> testCases, String code) {
        if (!inFlight.add(user.getId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Your previous submission is still running");
        }
        try {
            acquireSlot();
            try {
                return judge(user, problem, testCases, code);
            } finally {
                judgeSlots.release();
            }
        } finally {
            inFlight.remove(user.getId());
        }
    }

    // ------------------------------------------------------------------ judging

    private Submission judge(User user, Problem problem, List<TestCase> testCases, String code) {
        Submission s = new Submission();
        s.setUser(user);
        s.setProblem(problem);
        s.setCode(code);
        s.setTotalCount(testCases.size());

        try (CompiledProgram program = runner.compile(code, problem.getDriverCode())) {
            if (!program.isCompiled()) {
                String message = "Compilation error:\n" + program.getCompileError();
                return finish(s, SubmissionStatus.ERROR, 0, 0, message, message);
            }

            int passed = 0;
            long totalTimeMs = 0;
            SubmissionStatus failure = null;
            String message = null;
            String output = null;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                int number = i + 1;
                RunResult r = runner.run(program, tc.getInput());
                totalTimeMs += r.elapsedMs();
                output = r.stdout();

                if (r.verdict() == RunResult.Verdict.OK) {
                    if (sameOutput(r.stdout(), tc.getExpectedOutput())) {
                        passed++;
                    } else if (failure == null) {
                        failure = SubmissionStatus.WRONG_ANSWER;
                        message = wrongAnswerMessage(tc, number, testCases.size(), r.stdout());
                    }
                    continue;
                }

                // anything other than a plain wrong answer ends the run for TLE/memory/output,
                // while runtime errors still let the remaining cases earn partial credit
                SubmissionStatus status = r.verdict() == RunResult.Verdict.TIME_LIMIT_EXCEEDED
                        ? SubmissionStatus.TIME_LIMIT_EXCEEDED
                        : SubmissionStatus.ERROR;
                String detail = describe(r, number);

                if (failure == null || status == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
                    failure = status;
                    message = detail;
                    output = detail;
                }
                if (r.verdict() != RunResult.Verdict.RUNTIME_ERROR) {
                    break;
                }
            }

            SubmissionStatus status = failure != null ? failure : SubmissionStatus.ACCEPTED;
            return finish(s, status, passed, totalTimeMs, message, output);
        }
    }

    private Submission finish(Submission s, SubmissionStatus status, int passed, long timeMs,
                              String message, String storedOutput) {
        int total = s.getTotalCount();
        s.setStatus(status);
        s.setPassedCount(passed);
        s.setPassed(passed == total && status == SubmissionStatus.ACCEPTED);
        s.setScore(total == 0 ? 0 : (passed * 100) / total);
        s.setExecutionTimeMs(timeMs);
        s.setMessage(message);
        s.setUserOutput(truncate(storedOutput, MAX_STORED_OUTPUT));
        return s;
    }

    private static String describe(RunResult r, int testNumber) {
        return switch (r.verdict()) {
            case TIME_LIMIT_EXCEEDED -> "Time limit exceeded on test case " + testNumber + ".";
            case MEMORY_LIMIT_EXCEEDED -> "Memory limit exceeded on test case " + testNumber + ".";
            case OUTPUT_LIMIT_EXCEEDED -> "Output limit exceeded on test case " + testNumber + ".";
            default -> "Runtime error on test case " + testNumber + ":\n" + r.stderr();
        };
    }

    private static String wrongAnswerMessage(TestCase tc, int number, int total, String actual) {
        if (tc.isHidden()) {
            return "Wrong answer on hidden test case " + number + " of " + total + ".";
        }
        return "Wrong answer on test case " + number + " of " + total + ".\n"
                + "Input:\n" + nullToEmpty(tc.getInput()).strip() + "\n"
                + "Expected:\n" + nullToEmpty(tc.getExpectedOutput()).strip() + "\n"
                + "Your output:\n" + truncate(nullToEmpty(actual).strip(), 500);
    }

    /** Compares outputs ignoring trailing whitespace on each line and at the end. */
    static boolean sameOutput(String actual, String expected) {
        return normalise(actual).equals(normalise(expected));
    }

    private static String normalise(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            sb.append(line.stripTrailing()).append('\n');
        }
        return sb.toString().strip();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ---------------------------------------------------------------- throttling

    private static void enforceRateLimit(Long userId, Map<Long, Deque<Long>> history, int limitPerMinute) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = history.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > RATE_WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limitPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Please wait a minute and try again.");
            }
            timestamps.addLast(now);
        }
    }

    private void acquireSlot() {
        try {
            if (!judgeSlots.tryAcquire(WAIT_FOR_SLOT_SECONDS, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "The judge is busy right now. Please try again shortly.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The judge was interrupted");
        }
    }
}
