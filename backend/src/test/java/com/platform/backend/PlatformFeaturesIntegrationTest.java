package com.platform.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Run vs Submit, submission history, progress, statistics and the admin test-case endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class PlatformFeaturesIntegrationTest {

    private static final String DRIVER = """
            public static void main(String[] args) throws Exception {
                Scanner sc = new Scanner(System.in);
                System.out.print(add(sc.nextInt(), sc.nextInt()));
            }
            """;
    private static final String GOOD = "public static int add(int a, int b) { return a + b; }";
    private static final String WRONG = "public static int add(int a, int b) { return a - b; }";
    /** Right on the example (2+3) but wrong on the hidden case (10+20). */
    private static final String ONLY_EXAMPLE = "public static int add(int a, int b) { return a == 2 ? 5 : 0; }";

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

    private String admin(String name, String email) throws Exception {
        String token = register(name, email);
        User u = users.findByEmail(email).orElseThrow();
        u.setRole("ADMIN");
        users.save(u);
        return token;
    }

    private String submit(String token, String code) throws Exception {
        return mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(code))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // -------------------------------------------------------------------- Run

    @Test
    void runUsesOnlyExampleCasesAndStoresNothing() throws Exception {
        String token = register("Ann", "ann@example.com");

        // passes the visible example although it would fail the hidden case...
        mvc.perform(post("/submissions/" + problemId + "/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(ONLY_EXAMPLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.totalCount").value(1));

        // ...and a real submission of the same code is graded against all cases
        mvc.perform(post("/submissions/" + problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(ONLY_EXAMPLE))
                .andExpect(jsonPath("$.status").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.totalCount").value(2));

        assertEquals(1, submissions.count(), "only the graded submission may be stored, not the run");
    }

    @Test
    void runReportsWrongAnswerAndRequiresLogin() throws Exception {
        String token = register("Ben", "ben@example.com");
        mvc.perform(post("/submissions/" + problemId + "/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(WRONG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.message", containsString("Expected")));

        mvc.perform(post("/submissions/" + problemId + "/run")
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runIsRejectedWhenAProblemHasNoExampleCases() throws Exception {
        String token = register("Cy", "cy@example.com");
        testCases.findByProblemIdOrderByIdAsc(problemId).forEach(tc -> {
            tc.setHidden(true);
            testCases.save(tc);
        });
        mvc.perform(post("/submissions/" + problemId + "/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN).content(GOOD))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- history

    @Test
    void historyListsOwnSubmissionsNewestFirstWithoutCode() throws Exception {
        String token = register("Dee", "dee@example.com");
        submit(token, WRONG);
        submit(token, GOOD);

        mvc.perform(get("/submissions/mine?problemId=" + problemId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[1].status").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$[0].problemTitle").value("Add"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(content().string(not(containsString("a + b"))));

        mvc.perform(get("/submissions/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void submissionDetailShowsCodeButOnlyToItsOwner() throws Exception {
        String owner = register("Eli", "eli@example.com");
        String other = register("Fay", "fay@example.com");
        submit(owner, GOOD);
        Long id = submissions.findAll().get(0).getId();

        mvc.perform(get("/submissions/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(GOOD));

        mvc.perform(get("/submissions/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mvc.perform(get("/submissions/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/submissions/mine")).andExpect(status().isUnauthorized());

        // another user's history never contains it
        mvc.perform(get("/submissions/mine").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --------------------------------------------------------- progress / stats

    @Test
    void progressCountsSolvedProblemsByDifficulty() throws Exception {
        String token = register("Gus", "gus@example.com");
        mvc.perform(get("/me/progress").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.solvedCount").value(0))
                .andExpect(jsonPath("$.totalProblems").value(1))
                .andExpect(jsonPath("$.totalByDifficulty.EASY").value(1));

        submit(token, WRONG);
        mvc.perform(get("/me/progress").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.solvedCount").value(0))
                .andExpect(jsonPath("$.attemptedProblemIds", hasItem(problemId.intValue())));

        submit(token, GOOD);
        mvc.perform(get("/me/progress").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.solvedCount").value(1))
                .andExpect(jsonPath("$.solvedProblemIds", hasItem(problemId.intValue())))
                .andExpect(jsonPath("$.solvedByDifficulty.EASY").value(1))
                .andExpect(jsonPath("$.solvedByDifficulty.HARD").value(0))
                .andExpect(jsonPath("$.totalSubmissions").value(2))
                .andExpect(jsonPath("$.acceptedSubmissions").value(1))
                .andExpect(jsonPath("$.acceptanceRate").value(50))
                .andExpect(jsonPath("$.recent.length()").value(2))
                .andExpect(content().string(not(containsString("password"))));

        mvc.perform(get("/me/progress")).andExpect(status().isUnauthorized());
    }

    @Test
    void problemStatsArePublicAndAggregateOnly() throws Exception {
        String a = register("Hal", "hal@example.com");
        String b = register("Ivy", "ivy@example.com");
        submit(a, WRONG);
        submit(a, GOOD);
        submit(b, GOOD);
        submit(b, WRONG);

        mvc.perform(get("/problems/stats"))   // no token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(problemId.intValue()))
                .andExpect(jsonPath("$[0].submissions").value(4))
                .andExpect(jsonPath("$[0].accepted").value(2))
                .andExpect(jsonPath("$[0].acceptanceRate").value(50))
                .andExpect(content().string(not(containsString("hal@example.com"))));
    }

    // ------------------------------------------------------------------ admin

    @Test
    void adminCanSeeAndRemoveHiddenTestCasesCandidatesCannot() throws Exception {
        String candidate = register("Jo", "jo@example.com");
        String admin = admin("Kim", "kim@example.com");

        mvc.perform(get("/testcases/" + problemId + "/all").header("Authorization", "Bearer " + candidate))
                .andExpect(status().isForbidden());
        mvc.perform(get("/testcases/" + problemId + "/all")).andExpect(status().isUnauthorized());

        String body = mvc.perform(get("/testcases/" + problemId + "/all").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].hidden").value(true))
                .andExpect(jsonPath("$[1].expectedOutput").value("30"))
                .andReturn().getResponse().getContentAsString();
        Integer hiddenId = JsonPath.read(body, "$[1].id");

        mvc.perform(delete("/testcases/case/" + hiddenId).header("Authorization", "Bearer " + candidate))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/testcases/case/" + hiddenId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(delete("/testcases/case/" + hiddenId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        assertEquals(1, testCases.count());

        // the public example list never shows hidden cases, even to admins
        mvc.perform(get("/testcases/" + problemId)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminInputIsValidated() throws Exception {
        String admin = admin("Lee", "lee@example.com");

        mvc.perform(post("/problems").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"difficulty\":\"IMPOSSIBLE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/problems").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Good\",\"difficulty\":\"HARD\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("driverCode"))));

        mvc.perform(post("/testcases/" + problemId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"" + "x".repeat(300) + "\",\"expectedOutput\":\"1\",\"hidden\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/testcases/" + problemId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"1 1\",\"expectedOutput\":\"\",\"hidden\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/testcases/" + problemId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"1 1\",\"expectedOutput\":\"2\",\"hidden\":true}"))
                .andExpect(status().isOk());
    }
}
