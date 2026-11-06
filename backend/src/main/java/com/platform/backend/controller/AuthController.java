package com.platform.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.platform.backend.entity.User;
import com.platform.backend.repository.UserRepository;
import com.platform.backend.security.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores/rejects anything longer

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Used to spend the same time on unknown e-mails as on wrong passwords. */
    private final String dummyHash;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {
        String email = normaliseEmail(req.get("email"));
        String password = req.get("password");

        if (email == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        // one generic message for "no such user" and "wrong password" (no account enumeration)
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw invalidCredentials();
        }
        return session(user);
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> req) {
        String name = req.get("name") == null ? "" : req.get("name").trim();
        String email = normaliseEmail(req.get("email"));
        String password = req.get("password");

        if (name.isEmpty() || name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1-50 characters");
        }
        if (email == null || email.length() > 254 || !EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid email address");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("CANDIDATE"); // admins are promoted directly in the database
        userRepository.save(user);

        return session(user);
    }

    private Map<String, String> session(User user) {
        return Map.of(
                "token", jwtUtil.generateToken(user.getEmail()),
                "userId", String.valueOf(user.getId()),
                "name", user.getName(),
                "role", user.getRole());
    }

    private static String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
