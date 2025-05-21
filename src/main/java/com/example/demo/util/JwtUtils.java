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


    // 리프레시 토큰 유효기간 반환
    public long getRefreshTokenMillis() {
        return refreshTokenMillis;
    }


    // 액세스 토큰 유효기간 반환
    public long getAccessTokenMillis() {
        return accessTokenMillis;
    }


    /**
     * 토큰에서 usename(사용자 이름) 추출
     */ 
    public String getUsernameFromToken(String token) {
        
        return Jwts.parserBuilder()     // JWT 파서를 생성
            .setSigningKey(key)         // 서명을 검증할 키 설정
            .build()                    //
            .parseClaimsJws(token)      // 토큰 파싱 (서명 검증 포함)
            .getBody()                  // payload (Claims) 가져옴
            .getSubject();              // sub 필드가 username에 해당
    }


    /**
     * 토큰 유효성 검증 로직
     * 
     * parseClaimsJws(token) 호출 시, 내부적으로 다음 항목 점검
     *  1. 서명 검증 : 토큰이 signWith(..)로 서명된 비밀 키와 일치하는지 확인(위조 토큰 차단)
     *  2. 만료 시각 : 현재 시간보다 exp 값이 지났다면 expiredJwtException 발생
     *  3. 잘못된 형식/무결성 : 토큰의 구조적 손상이나 잘못된 포맷일 경우 IllegalArgumentException 발생
     * 
     * @param token : 접두어("bearer") 제거한 실제 토큰 문자열
     * @throws JwtException : 유효하지 않은 토큰일 경우 예외 발생
     */
    public void validation(String token) throws JwtException {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("토큰이 유효하지 않습니다.",ex);
        }
    }


    /**
     * Authorization 헤더에서 'Bearer ' 접두어를 제거하고 토큰 본문만 반환합니다.
     * 
     * @param bearerHeader 전체 Authorization 헤더 값 
     * @return 실제 토큰 문자열
     * @throws IllegalArgumentException 접두어가 없으면 예외 발생
     */
    public static String extractTokenFrom(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith(BEARER_PREFIX)) {
            throw new JwtException("Authorization 헤더 형식이 잘못되었습니다. 'Bearer {token}' 형식이어야 합니다.");
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