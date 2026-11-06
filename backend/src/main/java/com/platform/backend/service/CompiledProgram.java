package com.platform.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A submission that has been through the compiler. Owns a private temporary
 * directory which is deleted when the program is closed, so always use it in
 * a try-with-resources block.
 */
public final class CompiledProgram implements AutoCloseable {

    private final Path directory;
    private final String compileError;

    private CompiledProgram(Path directory, String compileError) {
        this.directory = directory;
        this.compileError = compileError;
    }

    static CompiledProgram success(Path directory) {
        return new CompiledProgram(directory, null);
    }

    static CompiledProgram failure(Path directory, String compileError) {
        return new CompiledProgram(directory, compileError);
    }

    public boolean isCompiled() {
        return compileError == null;
    }

    /** Compiler output (already sanitised) when compilation failed, otherwise null. */
    public String getCompileError() {
        return compileError;
    }

    Path getDirectory() {
        return directory;
    }

    @Override
    public void close() {
        deleteRecursively(directory);
    }

    static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort clean-up of a temp directory
                }
            });
        } catch (IOException ignored) {
            // best effort clean-up of a temp directory
        }
    }
}
