/*package com.platform.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

@Service
public class CodeRunnerService {

    public String runJava(String code, String input) throws Exception {

        File tempDir = Files.createTempDirectory("runner").toFile();

        File javaFile = new File(tempDir, "Solution.java");

        String wrappedCode =
                "import java.util.*;\n" +
                "public class Solution {\n" +
                "public static void main(String[] args) throws Exception {\n" +
                code + "\n" +
                "}\n}";

        Files.write(javaFile.toPath(), wrappedCode.getBytes());

        Process compile =
                new ProcessBuilder("javac", javaFile.getAbsolutePath())
                        .directory(tempDir)
                        .start();

        compile.waitFor();

        Process run =
                new ProcessBuilder("java", "Solution")
                        .directory(tempDir)
                        .redirectErrorStream(true)
                        .start();

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(run.getOutputStream()));
        writer.write(input);
        writer.flush();
        writer.close();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(run.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        run.waitFor();

        return output.toString().trim();
    }
}*/
package com.platform.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

@Service
public class CodeRunnerService {

    // 🔥 MAIN METHOD (used by SubmissionService)
    public String runCode(String code, String input) {
        try {
            return runJava(code, input);
        } catch (Exception e) {
            return "ERROR";
        }
    }

    // 🔧 Java execution logic
    private String runJava(String code, String input) throws Exception {

        File tempDir = Files.createTempDirectory("runner").toFile();

        File javaFile = new File(tempDir, "Solution.java");

        String wrappedCode =
                "import java.util.*;\n" +
                "public class Solution {\n" +
                "public static void main(String[] args) throws Exception {\n" +
                code + "\n" +
                "}\n}";

        Files.write(javaFile.toPath(), wrappedCode.getBytes());

        // compile
        Process compile =
                new ProcessBuilder("javac", javaFile.getAbsolutePath())
                        .directory(tempDir)
                        .start();

        compile.waitFor();

        // run
        Process run =
                new ProcessBuilder("java", "Solution")
                        .directory(tempDir)
                        .redirectErrorStream(true)
                        .start();

        // input pass
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(run.getOutputStream()));
        writer.write(input);
        writer.flush();
        writer.close();

        // output read
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(run.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        run.waitFor();

        return output.toString().trim();
    }
}
