package com.platform.backend.controller;

import com.platform.backend.entity.User;
import com.platform.backend.repository.UserRepository;
import com.platform.backend.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return Map.of("error", "User not found");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Map.of("error", "Invalid password");
        }

        String token = JwtUtil.generateToken(user.getEmail());
        return Map.of("token", token, "userId", String.valueOf(user.getId()), "name", user.getName());
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> req) {

        String name = req.get("name");
        String email = req.get("email");
        String password = req.get("password");

        if (userRepository.findByEmail(email).isPresent()) {
            return Map.of("error", "Email already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("CANDIDATE");

        userRepository.save(user);

        String token = JwtUtil.generateToken(user.getEmail());
        return Map.of("token", token, "userId", String.valueOf(user.getId()), "name", user.getName());
    }
}
