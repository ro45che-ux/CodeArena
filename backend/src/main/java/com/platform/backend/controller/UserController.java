package com.platform.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.platform.backend.entity.User;
import com.platform.backend.repository.UserRepository;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // create user
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // get all users
    @GetMapping
    public List<User> getUsers() {
        return userRepository.findAll();
    }
}

