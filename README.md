# 🏆 CodeArena | Full-Stack LeetCode Clone

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**CodeArena** is a full-stack, production-ready online coding judge (similar to LeetCode). It enables users to securely write, compile, and execute Java code against hidden test cases in real-time, with a dynamic leaderboard ranking system.

### 🌐 Live Demo
👉 **[View the Live Platform](https://code-arena-frontend.onrender.com)**

---

## 🚀 Key Features

* **Secure Code Execution Engine** – Dynamically compiles and executes user-submitted Java code using `ProcessBuilder` with strict timeouts and isolated temporary workspaces.
* **Automated Test Case Evaluation** – Validates user output against multiple hidden test cases directly from the PostgreSQL database.
* **Global Leaderboard** – Ranks users dynamically using `O(n log n)` sorting based on highest scores and lowest execution times.
* **Stateless Authentication** – Secured REST APIs using **Spring Security**, **JWT (HS256)**, and **BCrypt** password hashing.
* **Dockerized Deployment** – Fully containerized multi-tier architecture, currently hosted live on **Render**.

---

## 💻 Tech Stack

### **Backend**
* Java 21
* Spring Boot 3
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
3. **Sandbox Setup:** The `CodeRunnerService` creates an isolated temporary directory for the submission.
4. **Compile & Execute:** `ProcessBuilder` spawns a `javac` process to compile the code, and a `java` process to run it. The JVM's `JAVA_TOOL_OPTIONS` are scrubbed to prevent output pollution.
5. **Evaluate:** Standard I/O streams inject hidden test cases and capture the output. Timeouts (e.g., 5 seconds) are strictly enforced to prevent infinite loops (`while(true)`).
6. **Score:** Results are aggregated, saved to the PostgreSQL database, and the Global Leaderboard is updated instantly.

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

### 2. Start the Application
```bash
docker compose up --build -d
```
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

## 📖 Future Improvements
- [ ] Add support for multiple languages (Python, C++).
- [ ] Implement advanced containerized sandboxing per submission (e.g., Isolate/gVisor).
- [ ] Add explicit memory-limit tracking.
