#  CodeArena – Coding Platform

A full-stack coding platform built using **Spring Boot** and **React**, where users can solve problems, submit code, and get evaluated against test cases with a dynamic leaderboard system.

---

##  Features

*  Code Execution Engine** – Executes user-submitted code dynamically using Java ProcessBuilder
*  Test Case Evaluation** – Validates code against multiple test cases
*  Leaderboard System** – Ranks users based on best submission scores
*  JWT Authentication** – Secure APIs with token-based authentication
*  REST APIs** – Clean backend architecture using Spring Boot
*  Interactive UI** – Simple React interface for problem solving

---

##  Tech Stack

### Backend

* Java
* Spring Boot
* Spring Security (JWT)
* JPA / Hibernate

### Frontend

* React
* JavaScript
* CSS

### Database

* H2 (Development)
* MySQL (Optional for production)

---

##  How It Works

1. User logs in and receives a JWT token
2. User selects a problem and submits code
3. Backend executes code using a runtime environment
4. Output is validated against test cases
5. Score is calculated and stored
6. Leaderboard updates with best submissions

---

##  Project Structure

```
coding-platform/
│
├── backend/       # Spring Boot application
│
├── frontend/      # React application
│
└── README.md
```

---

##  Running Locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

---

##  Authentication

* Login via `/auth/login`
* JWT token is required for protected APIs (like submissions)

---

##  Limitations

* Code execution currently supports Java only
* Runs locally (not sandboxed like production systems)
* Deployment requires environment setup

---

##  Future Improvements

* Multi-language support (C++, Python)
* Docker-based secure execution
* Time & memory limits
* Public + hidden test cases
* Improved UI/UX

---

## ⭐ Final Note

This project demonstrates backend system design, API development, and real-world problem evaluation logic similar to platforms like LeetCode or CodeChef.
