package com.example.demo.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
                                              // 256 byte 이상
    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkeymysecretkey";
    private static final String BEARER_PREFIX = "Bearer ";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    private final long accessTokenMillis = 1000 * 60 * 30;              // 30 minutes
    private final long refreshTokenMillis = 1000 * 60 * 60 * 24 * 7;   //  7 days


    // Access Token 생성
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

    // 토큰에서 usename(사용자 이름) 추출
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

    // 리프레시 토큰 유효기간 반환
    public long getRefreshTokenMillis() {
        return refreshTokenMillis;
    }

    // 액세스 토큰 유효기간 반환
    public long getAccessTokenMillis() {
        return accessTokenMillis;
    }

    /**
     * Authorization 헤더에서 'Bearer ' 접두어를 제거하고 토큰 본문만 반환합니다.
     * @param bearerHeader 전체 Authorization 헤더 값 
     * @return 실제 토큰 문자열
     * @throws IllegalArgumentException 접두어가 없으면 예외 발생
     */
    public static String extractTokenFrom(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return bearerHeader.substring(BEARER_PREFIX.length());
    }    

    /**
     * Authorization 헤더 또는 Cookie에서 토큰을 추출합니다.
     * 헤더 우선 → 없으면 Cookie → 둘 다 없으면 예외 발생.
     *
     * @param authorizationHeader Authorization 헤더 (nullable)
     * @param refreshTokenCookie  쿠키 값 (nullable)
     * @return JWT 토큰 문자열
     * @throws ResponseStatusException 둘 다 없을 경우
     */
    public static String extractTokenFromHeaderOrCookie(String authorizationHeader, String refreshTokenCookie) {
        try {
            return extractTokenFrom(authorizationHeader);
        } catch (IllegalArgumentException e) {
            if (refreshTokenCookie != null) {
                return refreshTokenCookie;
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }
    }
    
}