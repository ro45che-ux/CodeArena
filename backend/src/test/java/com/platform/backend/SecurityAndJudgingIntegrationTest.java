package com.platform.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.platform.backend.entity.Problem;
import com.platform.backend.entity.TestCase;
import com.platform.backend.entity.User;
import com.platform.backend.repository.ProblemRepository;
import com.platform.backend.repository.SubmissionRepository;
import com.platform.backend.repository.TestCaseRepository;
import com.platform.backend.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndJudgingIntegrationTest {

    private static final String DRIVER = """
            public static void main(String[] args) throws Exception {
                Scanner sc = new Scanner(System.in);
                System.out.print(add(sc.nextInt(), sc.nextInt()));
            }
            """;
    private static final String GOOD = "public static int add(int a, int b) { return a + b; }";
    private static final String WRONG = "public static int add(int a, int b) { return a - b; }";

    @Autowired MockMvc mvc;
    @Autowired ProblemRepository problems;
    @Autowired TestCaseRepository testCases;
    @Autowired SubmissionRepository submissions;
    @Autowired UserRepository users;

    private Long problemId;

    @BeforeEach
    void seed() {
        submissions.deleteAll();
        testCases.deleteAll();
        problems.deleteAll();
        users.deleteAll();

        Problem p = new Problem();
        p.setTitle("Add");
        p.setDescription("Add two numbers");
        p.setDifficulty("EASY");
        p.setSolutionTemplate(GOOD);
        p.setDriverCode(DRIVER);
        problemId = problems.save(p).getId();

        addCase(p, "2 3", "5", false);
        addCase(p, "10 20", "30", true);
    }

    private void addCase(Problem p, String in, String out, boolean hidden) {
        TestCase tc = new TestCase();
        tc.setProblem(p);
        tc.setInput(in);
        tc.setExpectedOutput(out);
        tc.setHidden(hidden);
        testCases.save(tc);
    }

    private String register(String name, String email) throws Exception {
        String body = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String submit(String token, String code) throws Exception {
        return mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(code))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------------------------------------------------------- authentication

    @Test
    void submittingRequiresLogin() throws Exception {
        mvc.perform(post("/submissions/" + problemId).contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void forgedOrGarbageTokensAreRejected() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-secret-of-sufficient-length-1234".getBytes());
        String forged = Jwts.builder().setSubject("someone@example.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey, SignatureAlgorithm.HS256).compact();

        for (String token : new String[] {forged, "not.a.jwt"}) {
            mvc.perform(post("/submissions/" + problemId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        register("Alice", "alice@example.com");

        String wrongPassword = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"nope-nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"nope-nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(wrongPassword, unknownEmail);
    }

    @Test
    void registrationValidatesInputAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        register("Bob", "bob@example.com");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"BOB@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    // ----------------------------------------------------------- authorisation

    @Test
    void onlyAdminsCanCreateProblemsAndTestCases() throws Exception {
        String problemJson = "{\"title\":\"New\",\"description\":\"d\",\"difficulty\":\"EASY\"}";
        String testCaseJson = "{\"input\":\"1 1\",\"expectedOutput\":\"2\",\"hidden\":false}";

        mvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON).content(problemJson))
                .andExpect(status().isUnauthorized());

        String candidate = register("Carol", "carol@example.com");
        mvc.perform(post("/problems").header("Authorization", "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON).content(problemJson))
                .andExpect(status().isForbidden());
        mvc.perform(post("/testcases/" + problemId).header("Authorization", "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON).content(testCaseJson))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users").header("Authorization", "Bearer " + candidate))
                .andExpect(status().isForbidden());

        User carol = users.findByEmail("carol@example.com").orElseThrow();
        carol.setRole("ADMIN");
        users.save(carol);

        mvc.perform(post("/problems").header("Authorization", "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON).content(problemJson))
                .andExpect(status().isOk());
        mvc.perform(post("/testcases/" + problemId).header("Authorization", "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON).content(testCaseJson))
                .andExpect(status().isOk());
        mvc.perform(get("/users").header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void corsOnlyAllowsConfiguredOrigins() throws Exception {
        mvc.perform(options("/problems")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
        mvc.perform(options("/problems")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ data exposure

    @Test
    void publicEndpointsNeverLeakSecrets() throws Exception {
        String token = register("Dave", "dave@example.com");
        submit(token, GOOD);

        mvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("driverCode"))))
                .andExpect(content().string(not(containsString("Scanner"))));

        mvc.perform(get("/leaderboard/" + problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("Dave"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("dave@example.com"))))
                .andExpect(content().string(not(containsString("return a + b"))));

        mvc.perform(get("/testcases/" + problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))   // hidden case is not listed
                .andExpect(content().string(not(containsString("30"))));
    }

    @Test
    void submissionResponseDoesNotExposeAccountDetails() throws Exception {
        String token = register("Eve", "eve@example.com");
        String json = submit(token, GOOD);
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("password"), json);
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("eve@example.com"), json);
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("driverCode"), json);
    }

    // ---------------------------------------------------------------- judging

    @Test
    void correctSolutionIsAccepted() throws Exception {
        String token = register("Frank", "frank@example.com");
        mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.passedCount").value(2))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    void wrongSolutionGetsWrongAnswerWithoutRevealingHiddenCases() throws Exception {
        String token = register("Grace", "grace@example.com");
        mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(WRONG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.passedCount").value(0))
                .andExpect(jsonPath("$.message", containsString("Expected")))
                .andExpect(jsonPath("$.message", not(containsString("30"))));
    }

    @Test
    void compileErrorsAreReportedToTheUser() throws Exception {
        String token = register("Heidi", "heidi@example.com");
        mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content("public static int add(int a, int b) { return a +; }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message", containsString("Compilation error")))
                .andExpect(jsonPath("$.message", not(containsString("/tmp"))));
    }

    @Test
    void submissionsAreAttributedToTheTokenOwnerAndNoUserIdCanBeInjected() throws Exception {
        String token = register("Ivan", "ivan@example.com");
        register("Judy", "judy@example.com");
        Long judyId = users.findByEmail("judy@example.com").orElseThrow().getId();

        mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Ivan"));

        // the old /{problemId}/{userId} URL form no longer exists
        mvc.perform(post("/submissions/" + problemId + "/" + judyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidSubmissionsAreRejectedCleanly() throws Exception {
        String token = register("Karl", "karl@example.com");

        mvc.perform(post("/submissions/999999").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        mvc.perform(post("/submissions/" + problemId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content("x".repeat(5001)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/submissions/" + problemId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content("   "))
                .andExpect(status().isBadRequest());
    }
}
