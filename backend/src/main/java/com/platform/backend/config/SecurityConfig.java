package com.platform.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.platform.backend.repository.UserRepository;
import com.platform.backend.security.JwtFilter;
import com.platform.backend.security.JwtUtil;
import com.platform.backend.security.RequestSizeFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtUtil jwtUtil,
                                           UserRepository userRepository) throws Exception {
        http
            .cors(Customizer.withDefaults())  // uses the CorsConfigurationSource bean below
            .csrf(csrf -> csrf.disable())  // stateless API: JWT in a header, no cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeError(res, 401, "Authentication required"))
                .accessDeniedHandler((req, res, ex) -> writeError(res, 403, "You do not have permission to do that")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/hello", "/error").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // public, read-only
                .requestMatchers(HttpMethod.GET, "/problems", "/leaderboard/**", "/testcases/**").permitAll()
                // content management is admin only
                .requestMatchers(HttpMethod.POST, "/problems", "/testcases/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")
                // judging requires a logged-in user
                .requestMatchers("/submissions/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new RequestSizeFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Authentication is done by JwtFilter, so no UserDetailsService is needed. Declaring this
     * stops Spring Boot from creating a default user with a random password it prints to the log.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Password login is not used; authenticate with a JWT");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Only the configured front-end origins may call the API from a browser
     * ({@code CORS_ALLOWED_ORIGINS}, comma separated). Credentials/cookies are not used.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = allowedOrigins == null ? List.of()
                : Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res, int status, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
