package com.platform.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            "this_is_my_super_long_secret_key_for_hs256_jwt_generation_123456";

    private static final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ✅ generate token
    public static String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ extract username/email
    public static String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // ✅ validate token
    public static boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 🔧 helper
    private static Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}