package com.platform.backend.service;

/**
 * Source of the launcher class that is compiled next to every submission. It installs a
 * restrictive {@link java.security.Policy} plus the standard {@link SecurityManager} and then
 * calls the submission's {@code main}.
 *
 * <p>How the policy works: the JDK itself (boot/platform class loaders) keeps all permissions, so
 * lambdas, {@code Scanner}, streams, etc. behave normally. Code loaded from the class path, i.e.
 * the submission, only gets an allow-list: read the JDK and its own directory, read system
 * properties, and a few harmless runtime permissions. Because the standard access controller
 * intersects the permissions of every frame on the stack, the JDK cannot be used as a confused
 * deputy by user code: a call like {@code Files.readAllBytes("/etc/passwd")} is still denied.
 */
final class SandboxSources {

    private SandboxSources() {
    }

    static final String LAUNCHER = """
            import java.io.File;
            import java.io.FilePermission;
            import java.nio.file.Path;
            import java.security.Permission;
            import java.security.Policy;
            import java.security.ProtectionDomain;
            import java.util.ArrayList;
            import java.util.List;

            public final class SandboxLauncher {

                @SuppressWarnings({"removal", "deprecation"})
                public static void main(String[] args) throws Exception {
                    Policy.setPolicy(new JudgePolicy(args[0]));
                    System.setSecurityManager(new SecurityManager());
                    Solution.main(new String[0]);
                }

                @SuppressWarnings({"removal", "deprecation"})
                static final class JudgePolicy extends Policy {

                    private final List<Path> readableRoots = new ArrayList<>();
                    private final ClassLoader platformLoader = ClassLoader.getPlatformClassLoader();
                    private final ThreadLocal<Boolean> inCheck = new ThreadLocal<>();

                    JudgePolicy(String workDir) throws Exception {
                        addRoot(workDir);
                        addRoot(System.getProperty("java.home"));
                    }

                    private void addRoot(String dir) throws Exception {
                        Path p = Path.of(dir).toAbsolutePath().normalize();
                        readableRoots.add(p);
                        readableRoots.add(p.toRealPath());
                    }

                    @Override
                    public boolean implies(ProtectionDomain domain, Permission permission) {
                        ClassLoader loader = domain.getClassLoader();
                        if (loader == null || loader == platformLoader) {
                            return true; // the JDK itself
                        }
                        if (inCheck.get() != null) {
                            return true; // permission checks triggered while deciding
                        }
                        inCheck.set(Boolean.TRUE);
                        try {
                            return allowedForSubmission(permission);
                        } finally {
                            inCheck.remove();
                        }
                    }

                    private boolean allowedForSubmission(Permission p) {
                        String name = p.getName();

                        if (p instanceof FilePermission) {
                            // reading is allowed only inside the JDK and this submission's directory;
                            // writing, deleting and executing are never allowed
                            return "read".equals(p.getActions()) && readable(name);
                        }
                        if (p instanceof java.util.PropertyPermission) {
                            return "read".equals(p.getActions());
                        }
                        if (p instanceof RuntimePermission) {
                            return !(name.equals("setSecurityManager")
                                    || name.equals("createSecurityManager")
                                    || name.equals("createClassLoader")
                                    || name.equals("setIO")
                                    || name.equals("setFactory")
                                    || name.equals("manageProcess")
                                    || name.equals("writeFileDescriptor")
                                    || name.equals("readFileDescriptor")
                                    || name.startsWith("loadLibrary")
                                    || name.startsWith("getenv")
                                    || name.startsWith("defineClassInPackage")
                                    || name.startsWith("accessClassInPackage.sun")
                                    || name.startsWith("accessClassInPackage.jdk"));
                        }
                        // reflection hacks (suppressAccessChecks), sockets, URLs, logging control,
                        // management, AWT, security/serialization hooks, ... are all denied
                        return false;
                    }

                    private boolean readable(String name) {
                        if (name == null || name.equals("<<ALL FILES>>")) {
                            return false;
                        }
                        String n = name;
                        if (n.endsWith(File.separator + "-") || n.endsWith(File.separator + "*")) {
                            n = n.substring(0, n.length() - 2);
                        }
                        Path path;
                        try {
                            path = Path.of(n).toAbsolutePath().normalize();
                        } catch (Exception e) {
                            return false;
                        }
                        String s = path.toString();
                        if (s.equals("/dev/urandom") || s.equals("/dev/random") || s.equals("/dev/null")) {
                            return true;
                        }
                        for (Path root : readableRoots) {
                            if (path.startsWith(root)) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
            }
            """;
}
