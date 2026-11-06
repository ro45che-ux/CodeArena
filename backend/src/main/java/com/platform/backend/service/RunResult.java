package com.platform.backend.service;

/**
 * Outcome of running one compiled program against one test-case input.
 *
 * @param verdict   how the run ended
 * @param stdout    captured standard output (capped)
 * @param stderr    captured, sanitised error output (capped); useful for runtime-error messages
 * @param elapsedMs wall-clock time of the run in milliseconds
 */
public record RunResult(Verdict verdict, String stdout, String stderr, long elapsedMs) {

    public enum Verdict {
        OK,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED,
        RUNTIME_ERROR
    }
}
