/*package com.platform.backend.controller;

import com.platform.backend.entity.User;
import com.platform.backend.repository.UserRepository;
import com.platform.backend.security.JwtUtil;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;
    
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Bad credentials");
        }

        // TEMP token (JWT next step)
        String token = JwtUtil.generateToken(user.getEmail());
        return Map.of("token", token);
    }
    public String login(@RequestBody User user) {

        User dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            return JwtUtil.generateToken(user.getUsername());
        }

        throw new RuntimeException("Invalid credentials");
}
}*/
package com.platform.backend.controller;

import com.platform.backend.entity.User;
import com.platform.backend.repository.UserRepository;
import com.platform.backend.security.JwtUtil;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
public Map<String, String> login(@RequestBody Map<String, String> req) {

    String email = req.get("email");
    String password = req.get("password");

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
        return Map.of("error", "User not found");
    }

    if (!user.getPassword().equals(password)) {
        return Map.of("error", "Invalid password");
    }

    String token = JwtUtil.generateToken(user.getEmail());

    return Map.of("token", token);
}
}
