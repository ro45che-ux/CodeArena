package com.platform.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // System.out.println("========== JWT FILTER ==========");
        // System.out.println("URI: " + request.getRequestURI());
        // System.out.println("Method: " + request.getMethod());

        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();

        if (uri.startsWith("/auth")
                || uri.startsWith("/problems")
                || uri.startsWith("/leaderboard")
                || uri.startsWith("/testcases")
                || uri.startsWith("/submissions")) {

            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        // System.out.println("Authorization Header: " + header);

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            boolean valid = JwtUtil.isTokenValid(token);
            // System.out.println("Token Valid: " + valid);

            if (valid) {

                String email = JwtUtil.extractEmail(token);
                // System.out.println("Authenticated User: " + email);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, List.of());

                SecurityContextHolder.getContext().setAuthentication(auth);

                // System.out.println("Authentication Set Successfully");
            }
        }

        filterChain.doFilter(request, response);
    }
}