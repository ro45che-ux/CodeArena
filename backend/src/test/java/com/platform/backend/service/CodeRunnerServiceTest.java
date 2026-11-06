package com.platform.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.platform.backend.service.RunResult.Verdict;

/**
 * Runs real javac/java processes, so these tests double as a check of the sandbox: each hostile
 * program must be stopped by one of the judge's layers.
 */
class CodeRunnerServiceTest {

    private static final String ECHO_DRIVER = """
            public static void main(String[] args) throws Exception {
                Scanner sc = new Scanner(System.in);
                int a = sc.nextInt();
                int b = sc.nextInt();
                System.out.print(add(a, b));
            }
            """;

    @BeforeAll
    static void needsSecurityManagerCapableJvm() {
        assumeTrue(Runtime.version().feature() <= 23,
                "The in-JVM sandbox needs Java 23 or older; run these tests with JAVA_HOME on Java 17-21");
    }

    private final CodeRunnerService runner = new CodeRunnerService(3000, 30000, 64, true, false);

    private RunResult runUser(String userCode, String input) {
        try (CompiledProgram p = runner.compile(userCode, ECHO_DRIVER)) {
            assertTrue(p.isCompiled(), "compile failed: " + p.getCompileError());
            return runner.run(p, input);
        }
    }

    /** User code whose add() body is replaced by {@code body}. */
    private RunResult runBody(String body) {
        return runUser("public static int add(int a, int b) {\n" + body + "\n}", "2 3");
    }

    // ------------------------------------------------------------- happy path

    @Test
    void correctSolutionProducesOutput() {
        RunResult r = runBody("return a + b;");
        assertEquals(Verdict.OK, r.verdict());
        assertEquals("5", r.stdout().trim());
    }

    @Test
    void commonJdkFeaturesWorkInsideTheSandbox() {
        RunResult r = runBody("""
                java.util.List<Integer> xs = new ArrayList<>(List.of(5, 3, 1));
                Collections.sort(xs);
                Map<String, Integer> m = new HashMap<>();
                m.put("k", xs.get(0));
                PriorityQueue<Integer> pq = new PriorityQueue<>(xs);
                String s = String.format("%d-%s", pq.poll(), xs.stream().map(String::valueOf).collect(Collectors.joining(",")));
                int[] arr = {3, 1, 2};
                Arrays.sort(arr);
                Optional<Integer> max = xs.stream().max(Integer::compare);
                Random rnd = new Random(1);
                StringBuilder sb = new StringBuilder(s).append(arr[0]).append(max.get()).append(rnd.nextInt(1));
                Thread t = new Thread(() -> m.put("t", 1));
                t.start();
                try { t.join(); } catch (InterruptedException e) { }
                System.err.println("debug output on stderr is fine");
                return a + b;
                """);
        assertEquals(Verdict.OK, r.verdict(), r.stderr());
        assertEquals("5", r.stdout().trim());
    }

    @Test
    void userImportsAreHoistedAndLineNumbersMatchTheEditor() {
        String user = "import java.util.concurrent.atomic.AtomicInteger;\n"
                + "public static int add(int a, int b) {\n"
                + "    AtomicInteger x = new AtomicInteger(a);\n"
                + "    return x.get() + b + undefinedSymbol;\n"
                + "}";
        try (CompiledProgram p = runner.compile(user, ECHO_DRIVER)) {
            assertFalse(p.isCompiled());
            assertTrue(p.getCompileError().contains("Solution.java:4:"), p.getCompileError());
            assertTrue(p.getCompileError().contains("undefinedSymbol"), p.getCompileError());
        }
    }

    @Test
    void compileErrorsDoNotRevealServerPaths() {
        try (CompiledProgram p = runner.compile("this is not java", ECHO_DRIVER)) {
            assertFalse(p.isCompiled());
            assertNotNull(p.getCompileError());
            assertFalse(p.getCompileError().contains("judge-"), p.getCompileError());
            assertFalse(p.getCompileError().contains("/tmp"), p.getCompileError());
        }
    }

    @Test
    void uncaughtExceptionIsARuntimeError() {
        RunResult r = runBody("throw new IllegalStateException(\"boom\");");
        assertEquals(Verdict.RUNTIME_ERROR, r.verdict());
        assertTrue(r.stderr().contains("boom"), r.stderr());
    }

    // ------------------------------------------------------------ resource limits

    @Test
    void silentInfiniteLoopIsKilledAfterTheTimeout() {
        long start = System.currentTimeMillis();
        RunResult r = runBody("while (true) { }");
        assertEquals(Verdict.TIME_LIMIT_EXCEEDED, r.verdict());
        assertTrue(System.currentTimeMillis() - start < 15000, "judge must not hang");
    }

    @Test
    void memoryHogIsStopped() {
        RunResult r = runBody("List<long[]> hog = new ArrayList<>(); while (true) hog.add(new long[1_000_000]);");
        assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, r.verdict());
    }

    @Test
    void endlessOutputIsCutOff() {
        long start = System.currentTimeMillis();
        RunResult r = runBody("while (true) System.out.println(\"spam spam spam spam spam spam\");");
        assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, r.verdict());
        assertTrue(System.currentTimeMillis() - start < 15000);
    }

    // ------------------------------------------------------------- sandbox rules

    @Test
    void environmentVariablesAreNotVisible() {
        RunResult r = runBody("""
                System.out.print(System.getenv().size());
                return 0;
                """);
        // reading the environment is denied outright; even if it were allowed it would be empty
        assertTrue(r.verdict() == Verdict.RUNTIME_ERROR || "0".equals(r.stdout().trim()), r.stdout());
    }

    @Test
    void readingFilesOutsideTheSandboxIsDenied() {
        RunResult r = runBody("""
                try { System.out.print(new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("/etc/passwd")))); }
                catch (Throwable t) { System.out.print("BLOCKED " + t.getClass().getSimpleName()); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
        assertFalse(r.stdout().contains("root:"), r.stdout());
    }

    @Test
    void readingTheParentProcessEnvironmentIsDenied() {
        RunResult r = runBody("""
                try { System.out.print(new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("/proc/" + ProcessHandle.current().parent().get().pid() + "/environ")))); }
                catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }

    @Test
    void writingFilesIsDenied() {
        RunResult r = runBody("""
                try { java.nio.file.Files.writeString(java.nio.file.Path.of("pwned.txt"), "x"); System.out.print("WRITTEN"); }
                catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }

    @Test
    void startingProcessesIsDenied() {
        RunResult r = runBody("""
                try { Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "echo hi"}); System.out.print("SPAWNED"); }
                catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }

    @Test
    void networkAccessIsDenied() {
        RunResult r = runBody("""
                try { new java.net.Socket("example.com", 80).close(); System.out.print("CONNECTED"); }
                catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }

    @Test
    void userCodeCannotRemoveTheSecurityManager() {
        RunResult r = runBody("""
                try { System.setSecurityManager(null); System.out.print("REMOVED"); }
                catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }

    @Test
    void reflectionCannotBreakIntoJdkInternals() {
        RunResult r = runBody("""
                try {
                    var f = System.class.getDeclaredField("security");
                    f.setAccessible(true);
                    System.out.print("ACCESSIBLE");
                } catch (Throwable t) { System.out.print("BLOCKED"); }
                return 0;
                """);
        assertTrue(r.stdout().startsWith("BLOCKED"), r.stdout());
    }
}
