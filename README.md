# 🏆 CodeArena | Full-Stack LeetCode Clone

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**CodeArena** is a full-stack online coding judge (similar to LeetCode). It lets users write, compile, and execute Java code against hidden test cases, with a per-problem leaderboard.

### 🌐 Live Demo
👉 **[View the Live Platform](https://code-arena-frontend.onrender.com)**

---

## 🚀 Key Features

* **Sandboxed Code Execution Engine** – Compiles each submission once and runs it per test case in a locked-down child JVM: empty environment, Java Security Manager policy (no network, no process creation, no file writes, no reads outside the JDK and its own temp directory), heap/stack/CPU limits, wall-clock timeout and output cap. See [Security model](#-security-model) for what this does and does not protect against.
* **Clear Verdicts** – Accepted, Wrong Answer, Time Limit Exceeded, and Error (compile errors with line numbers, runtime errors, memory/output limits).
* **Automated Test Case Evaluation** – Validates user output against multiple hidden test cases directly from the PostgreSQL database.
* **Leaderboard** – Best submission per user for each problem, ranked by score and then by run time. The public API exposes only display names and scores.
* **Stateless Authentication** – Secured REST APIs using **Spring Security**, **JWT (HS256)**, and **BCrypt** password hashing.
* **Dockerized Deployment** – Fully containerized multi-tier architecture, currently hosted live on **Render**.

---

## 💻 Tech Stack

### **Backend**
* Java 21
* Spring Boot 4
* Spring Security (JWT + BCrypt)
* Spring Data JPA (Hibernate)

### **Frontend**
* React.js
* TailwindCSS / Vanilla CSS
* REST API Integration

### **Database & DevOps**
* PostgreSQL
* Docker & Docker Compose
* Nginx (Frontend Serving)
* Render (Cloud Deployment)

---

## ⚙️ Architecture & Execution Flow

1. **Submit:** User submits code via the React frontend.
2. **Authenticate:** Backend intercepts the request and validates the JWT using a custom `OncePerRequestFilter`.
3. **Authorize:** The submitting user is taken from the verified token (never from the URL). Per-user rate limiting and a global concurrency cap protect the judge.
4. **Compile once:** `CodeRunnerService` writes the code into a private temp directory and compiles it a single time (annotation processing off). Compiler errors keep your editor's line numbers and never reveal server paths.
5. **Run per test case:** For each test case a fresh, sandboxed `java` process is started with an empty environment and a Security Manager policy; stdin is fed the test input, stdout/stderr are captured with size caps, and the whole process tree is killed on timeout (so a silent `while(true)` cannot hang the server).
6. **Score:** Outputs are compared (trailing whitespace ignored), the verdict and score are saved to PostgreSQL, and the leaderboard reflects the best attempt per user.

---

## 🛠️ Local Setup Instructions

You can run the entire platform on your local machine using a single Docker command!

### Prerequisites
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### 1. Clone the Repository
```bash
git clone https://github.com/ro45che-ux/CodeArena.git
cd CodeArena
```

### 2. Configure and Start the Application
```bash
cp .env.example .env     # then edit .env: set POSTGRES_PASSWORD and JWT_SECRET
docker compose up --build -d
```
Every variable has a development default, so this also works without a `.env`, but **always set real values before exposing the app to the internet**.
*This command will automatically download PostgreSQL, build the Spring Boot backend, and build the React frontend.*

### 3. Access the Platform
* **Frontend:** `http://localhost:3000`
* **Backend API:** `http://localhost:8080`
* **Database (Postgres):** `localhost:5432`

### 4. Stop the Application
```bash
docker compose down
```

---

## 📚 Loading Problems

The database starts empty. Problems and their test cases live in `db/`:

| File | Contents |
|---|---|
| `db/init.sql` | Creates the tables and 2 starter problems (Two Sum, Reverse String). **Deletes existing problems first**, so use it only on an empty database. |
| `db/seed_problems.sql` | 13 more problems (Easy to Hard), 80 test cases (42 hidden). **Safe to re-run**: it skips problems that already exist and never deletes anything. |

With `docker compose up`, both files load automatically on the first start. On a hosted database (e.g. Render), start the backend once so it creates the tables, then run from your own machine:

```bash
psql "<EXTERNAL_DATABASE_URL>" -f db/seed_problems.sql
```

Every reference solution in `seed_problems.sql` was run through the real judge against all of its test cases before the file was generated.

---

## 🔐 Security Model

| Area | What is in place |
|---|---|
| Authentication | JWT (HS256) signed with `JWT_SECRET` from the environment (never committed); BCrypt password hashes; generic "invalid email or password" message; 401/403 JSON errors |
| Authorization | Submissions need a login and are attributed to the token owner. Creating problems/test cases and listing users is `ADMIN` only. Roles are read from the database on every request |
| Data exposure | Password hashes, e-mails, other users' code and the problem driver code are never returned. Hidden test cases are never listed; wrong answers on hidden cases reveal no input/expected output |
| CORS | Only origins in `CORS_ALLOWED_ORIGINS` |
| Judge isolation | Empty environment (no DB password/JWT secret visible), Security Manager policy for submitted code, heap/stack/CPU/output/time limits, runs as an unprivileged OS user in Docker |
| Abuse control | 10 submissions/minute/user, one running submission per user, at most 2 judged at once (`JUDGE_*` variables), request-size limit |

**Making someone an admin:** register normally, then run `UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';`

**Honest limitations.** The in-JVM sandbox raises the bar a lot, but it is not a substitute for OS-level isolation:
* The Java Security Manager is deprecated and was **removed in Java 24**. The judge is pinned to Java 21 and **refuses to start on Java 24+** unless `JUDGE_ALLOW_UNSANDBOXED=true` (local development only).
* A JVM-level sandbox can have undiscovered bypasses. For hostile, public traffic, run each submission in its own container (no network, read-only filesystem, cgroup memory/CPU/PID limits) or use a dedicated judge such as Judge0/isolate.
* Docker-in-Docker is not available on Render's standard web services, so the hosted demo relies on the in-JVM sandbox plus the limits above. Keep the demo's database credentials unique to it.
* Memory is limited by `-Xmx` (heap) only; there is no cgroup limit per submission.

## ⚙️ Configuration

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection | local dev values |
| `JWT_SECRET` | Token signing secret, 32+ chars. **Set in production** | random per start |
| `CORS_ALLOWED_ORIGINS` | Comma-separated front-end origins | localhost + the Render front-end |
| `JUDGE_RUN_TIMEOUT_MS` / `JUDGE_MEMORY_MB` | Per-test time and heap limits | 5000 / 128 |
| `JUDGE_MAX_CONCURRENT` / `JUDGE_SUBMISSIONS_PER_MINUTE` | Throttling | 2 / 10 |
| `JUDGE_ALLOW_UNSANDBOXED` | Allow running without the Security Manager (Java 24+, dev only) | `false` |
| `REACT_APP_API_URL` | Backend URL baked into the front-end build | the Render backend |

## 🧪 Tests

```bash
cd backend && mvn test      # use JDK 17-21 (the sandbox tests are skipped on Java 24+)
```
The suite compiles and runs real hostile programs (infinite loops, memory hogs, output floods, file/network/process/reflection access, reading the parent's environment) and checks auth, authorization, CORS and data-exposure rules end to end.

---

## 📖 Future Improvements
- [ ] Add support for multiple languages (Python, C++).
- [ ] Per-submission container isolation (Isolate/gVisor/Docker) in addition to the in-JVM sandbox.
- [ ] Add explicit memory-limit tracking.
