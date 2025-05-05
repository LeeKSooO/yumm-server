package com.example.demo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
//import java.util.function.Function;

@Component
public class JwtUtils {
                                            // 256 byte 이상
    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkeymysecretkey";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    // Token 유효기간 set
    private final long accessTokenMillis = 1000 * 60;         // 1 hour -> 1분으로 수정(for test)
    private final long refreshTokenMillis = 1000 * 60 * 60 * 24 * 7; // 7 days

    // JWT 토큰 생성
    public String generateAccessToken(String username) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + accessTokenMillis))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + refreshTokenMillis))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    // JWT 토큰에서 usename(사용자 이름) 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    // Token Validation Check
    public boolean validation(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 리프레시 토큰의 유효기간(밀리초) 반환
    public long getRefreshTokenMillis() {
        return refreshTokenMillis;
    }

    // 액세스 토큰 유효기간 반환
    public long getAccessTokenMillis() {
        return accessTokenMillis;
    }

}