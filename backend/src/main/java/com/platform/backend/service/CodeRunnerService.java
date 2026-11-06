package com.platform.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Compiles and runs untrusted Java submissions.
 *
 * <p>Defence in depth (no single layer is trusted on its own):
 * <ul>
 *   <li>every submission gets its own private temp directory, deleted afterwards;</li>
 *   <li>the child JVM starts with an <b>empty environment</b>, so DB credentials and the JWT secret
 *       of the backend are never visible through {@code System.getenv()};</li>
 *   <li>a Java {@link SecurityManager} (installed by a tiny launcher class) blocks network access,
 *       process creation, file writes, reading anything outside the JDK and the work directory,
 *       reflection hacks and native libraries;</li>
 *   <li>hard limits: wall-clock timeout (enforced from the parent, so silent infinite loops are
 *       killed too), heap size, stack size, captured stdout/stderr size and a CPU-seconds
 *       {@code ulimit} as a backstop;</li>
 *   <li>compilation happens once per submission and runs with annotation processing disabled.</li>
 * </ul>
 *
 * <p>This is a best-effort sandbox, not a replacement for OS-level isolation (a container per
 * submission with no network, a read-only filesystem and cgroup limits). See the README.
 */
@Service
public class CodeRunnerService {

    private static final Logger log = LoggerFactory.getLogger(CodeRunnerService.class);

    private static final int MAX_STDOUT_BYTES = 64 * 1024;
    private static final int MAX_STDERR_BYTES = 8 * 1024;
    private static final int MAX_COMPILER_OUTPUT_BYTES = 16 * 1024;
    private static final int MAX_ERROR_MESSAGE_CHARS = 2000;
    private static final int MAX_ERROR_MESSAGE_LINES = 15;

    /** Last Java version that still lets us install a SecurityManager (JEP 486 removes it in 24). */
    private static final int LAST_JAVA_WITH_SECURITY_MANAGER = 23;

    private static final Pattern IMPORT_LINE =
            Pattern.compile("^\\s*import\\s+(static\\s+)?[\\w.]+(\\.\\*)?\\s*;\\s*$");

    private static final String DEFAULT_IMPORTS =
            "import java.util.*;import java.util.function.*;import java.util.stream.*;import java.io.*;";

    private final long runTimeoutMs;
    private final long compileTimeoutMs;
    private final int memoryMb;
    private final boolean securityManagerEnabled;

    public CodeRunnerService(
            @Value("${judge.run-timeout-ms:5000}") long runTimeoutMs,
            @Value("${judge.compile-timeout-ms:20000}") long compileTimeoutMs,
            @Value("${judge.memory-mb:128}") int memoryMb,
            @Value("${judge.use-security-manager:true}") boolean useSecurityManager,
            @Value("${judge.allow-unsandboxed:false}") boolean allowUnsandboxed) {
        this.runTimeoutMs = runTimeoutMs;
        this.compileTimeoutMs = compileTimeoutMs;
        this.memoryMb = memoryMb;

        boolean enable = useSecurityManager;
        if (enable && Runtime.version().feature() > LAST_JAVA_WITH_SECURITY_MANAGER) {
            // Fail closed: running untrusted code with no sandbox must be an explicit decision.
            if (!allowUnsandboxed) {
                throw new IllegalStateException("The judge sandbox needs Java " + LAST_JAVA_WITH_SECURITY_MANAGER
                        + " or older (this JVM is Java " + Runtime.version().feature() + "). Run the backend on "
                        + "Java 21 (see backend/Dockerfile), or for LOCAL DEVELOPMENT ONLY set "
                        + "JUDGE_ALLOW_UNSANDBOXED=true.");
            }
            enable = false;
        }
        if (!enable) {
            log.error("*** Judge sandbox is DISABLED: submissions run with only an empty environment and "
                    + "time/memory limits. Never expose this configuration to the internet. ***");
        }
        this.securityManagerEnabled = enable;
    }

    // ------------------------------------------------------------------ compile

    /**
     * Compiles the user's code together with the problem's driver code. Never throws for problems
     * in the user's code: those are reported through {@link CompiledProgram#getCompileError()}.
     *
     * @throws IllegalStateException if the judge itself fails (I/O error, interrupted, ...)
     */
    public CompiledProgram compile(String userCode, String driverCode) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("judge-").toRealPath();
            Files.writeString(dir.resolve("Solution.java"),
                    buildSource(userCode, driverCode), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("SandboxLauncher.java"),
                    SandboxSources.LAUNCHER, StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>(List.of(
                    toolPath("javac"),
                    "-J-Xmx256m", "-J-XX:+UseSerialGC", "-J-XX:TieredStopAtLevel=1",
                    "-proc:none", "-nowarn", "-Xlint:none",
                    "-encoding", "UTF-8",
                    "-d", dir.toString(),
                    "Solution.java", "SandboxLauncher.java"));

            Execution result = execute(command, dir, null, compileTimeoutMs,
                    MAX_COMPILER_OUTPUT_BYTES, MAX_COMPILER_OUTPUT_BYTES);

            if (result.timedOut()) {
                return CompiledProgram.failure(dir, "Compilation timed out.");
            }
            if (result.exitCode() != 0) {
                String output = (result.stdout() + "\n" + result.stderr()).trim();
                return CompiledProgram.failure(dir, clean(output, dir));
            }
            return CompiledProgram.success(dir);
        } catch (IOException e) {
            CompiledProgram.deleteRecursively(dir);
            throw new IllegalStateException("Judge could not prepare the submission", e);
        } catch (InterruptedException e) {
            CompiledProgram.deleteRecursively(dir);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Judge was interrupted", e);
        }
    }

    // ---------------------------------------------------------------------- run

    /** Runs an already compiled program once, feeding {@code input} to its standard input. */
    public RunResult run(CompiledProgram program, String input) {
        if (!program.isCompiled()) {
            throw new IllegalArgumentException("Program did not compile");
        }
        Path dir = program.getDirectory();

        List<String> command = new ArrayList<>();
        if (cpuLimitAvailable()) {
            long cpuSeconds = TimeUnit.MILLISECONDS.toSeconds(runTimeoutMs) + 5;
            command.addAll(List.of("/bin/sh", "-c",
                    "ulimit -t " + cpuSeconds + " 2>/dev/null; exec \"$@\"", "sh"));
        }
        command.add(toolPath("java"));
        command.addAll(List.of(
                "-Xmx" + memoryMb + "m",
                "-Xss1m",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-XX:MaxMetaspaceSize=96m",
                "-XX:-UsePerfData",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8"));
        if (securityManagerEnabled) {
            command.add("-Djava.security.manager=allow");
        }
        command.addAll(List.of("-cp", dir.toString()));
        if (securityManagerEnabled) {
            command.addAll(List.of("SandboxLauncher", dir.toString()));
        } else {
            command.add("Solution");
        }

        try {
            Execution r = execute(command, dir, input == null ? "" : input, runTimeoutMs,
                    MAX_STDOUT_BYTES, MAX_STDERR_BYTES);

            String stderr = clean(r.stderr(), dir);

            if (r.timedOut() || r.exitCode() == 152 /* SIGXCPU from ulimit -t */) {
                return new RunResult(RunResult.Verdict.TIME_LIMIT_EXCEEDED, "", "", r.elapsedMs());
            }
            if (r.outputOverflow()) {
                return new RunResult(RunResult.Verdict.OUTPUT_LIMIT_EXCEEDED, "", "", r.elapsedMs());
            }
            if (r.exitCode() == 0) {
                return new RunResult(RunResult.Verdict.OK, r.stdout(), stderr, r.elapsedMs());
            }
            if (stderr.contains("OutOfMemoryError")) {
                return new RunResult(RunResult.Verdict.MEMORY_LIMIT_EXCEEDED, "", "", r.elapsedMs());
            }
            return new RunResult(RunResult.Verdict.RUNTIME_ERROR, r.stdout(), stderr, r.elapsedMs());
        } catch (IOException e) {
            throw new IllegalStateException("Judge could not start the program", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Judge was interrupted", e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Builds {@code Solution.java}. The first line of the user's code sits on line 1 of the file
     * (header, imports and class declaration share that line) so compiler line numbers match the
     * editor. Import statements typed by the user are hoisted above the class.
     */
    static String buildSource(String userCode, String driverCode) {
        String normalised = (userCode == null ? "" : userCode).replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder imports = new StringBuilder(DEFAULT_IMPORTS);
        StringBuilder body = new StringBuilder();

        String[] lines = normalised.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (IMPORT_LINE.matcher(lines[i]).matches()) {
                imports.append(lines[i].trim());
            } else {
                body.append(lines[i]);
            }
            if (i < lines.length - 1) {
                body.append('\n');
            }
        }

        return imports + "public class Solution {" + body + "\n"
                + (driverCode == null ? "" : driverCode) + "\n}\n";
    }

    private boolean cpuLimitAvailable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return !os.contains("win") && Files.isExecutable(Path.of("/bin/sh"));
    }

    private static String toolPath(String tool) {
        Path candidate = Path.of(System.getProperty("java.home"), "bin", tool);
        return Files.isExecutable(candidate) ? candidate.toString() : tool;
    }

    /** Strips the private work-directory path, JVM warnings and trims to a UI-friendly size. */
    private static String clean(String text, Path dir) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String withoutPaths = text.replace(dir.toString() + "/", "").replace(dir.toString(), "");
        StringBuilder out = new StringBuilder();
        int lines = 0;
        for (String line : withoutPaths.split("\\R")) {
            if (line.startsWith("WARNING:") || line.startsWith("Picked up ")) {
                continue;
            }
            if (lines++ >= MAX_ERROR_MESSAGE_LINES) {
                out.append("...\n");
                break;
            }
            out.append(line).append('\n');
        }
        String result = out.toString().trim();
        return result.length() > MAX_ERROR_MESSAGE_CHARS
                ? result.substring(0, MAX_ERROR_MESSAGE_CHARS) + "..."
                : result;
    }

    private record Execution(int exitCode, boolean timedOut, boolean outputOverflow,
                             String stdout, String stderr, long elapsedMs) {
    }

    /**
     * Starts a process with an empty environment, feeds it stdin, captures capped stdout/stderr
     * and kills the whole process tree if it exceeds the timeout or output limit.
     */
    private Execution execute(List<String> command, Path workDir, String stdin, long timeoutMs,
                              int stdoutLimit, int stderrLimit)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command).directory(workDir.toFile());
        pb.environment().clear();                      // no secrets, no JAVA_TOOL_OPTIONS
        pb.environment().put("LANG", "C.UTF-8");

        long start = System.nanoTime();
        Process process = pb.start();

        AtomicBoolean overflow = new AtomicBoolean(false);
        StreamCapture out = StreamCapture.start(process.getInputStream(), stdoutLimit, process, overflow);
        StreamCapture err = StreamCapture.start(process.getErrorStream(), stderrLimit, process, overflow);

        try (OutputStream os = process.getOutputStream()) {
            if (stdin != null) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // the program may exit (or be killed) without reading its input
        }

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            killTree(process);
            process.waitFor(2, TimeUnit.SECONDS);
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        out.join();
        err.join();

        int exit = process.isAlive() ? -1 : process.exitValue();
        return new Execution(exit, !finished, overflow.get(), out.text(), err.text(), elapsedMs);
    }

    private static void killTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    /** Reads a stream on a daemon thread, keeping at most {@code limit} bytes. */
    private static final class StreamCapture {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private Thread thread;

        static StreamCapture start(InputStream in, int limit, Process process, AtomicBoolean overflow) {
            StreamCapture capture = new StreamCapture();
            capture.thread = new Thread(() -> {
                byte[] chunk = new byte[4096];
                try (InputStream stream = in) {
                    int n;
                    while ((n = stream.read(chunk)) != -1) {
                        int room = limit - capture.buffer.size();
                        if (n > room) {
                            capture.buffer.write(chunk, 0, Math.max(room, 0));
                            overflow.set(true);
                            killTree(process);
                            return;
                        }
                        capture.buffer.write(chunk, 0, n);
                    }
                } catch (IOException ignored) {
                    // stream closed because the process was killed
                }
            }, "judge-output-reader");
            capture.thread.setDaemon(true);
            capture.thread.start();
            return capture;
        }

        void join() throws InterruptedException {
            thread.join(2000);
        }

        String text() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
