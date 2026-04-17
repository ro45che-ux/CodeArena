package com.platform.backend.config;

import com.platform.backend.security.JwtFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // ✅ public APIs
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/problems", "/leaderboard/**").permitAll()

                // 🔒 protected APIs
                .anyRequest().authenticated()
            )

            // 🔥 JWT filter
            .addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}